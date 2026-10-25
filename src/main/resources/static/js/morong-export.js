/**
 * MODULE 17: GIAI ĐOẠN 3 - XUẤT EXCEL VÀ PDF (HCMUTE PORTAL)
 * File: morong-export.js
 * Quản lý dropdown "Xuất" và xử lý tải file an toàn qua JavaScript.
 */
document.addEventListener("DOMContentLoaded", function () {
  // Toast container singleton
  let toastContainer = document.querySelector(".morong-export-toast-container");
  function getToastContainer() {
    if (!toastContainer) {
      toastContainer = document.createElement("div");
      toastContainer.className = "morong-export-toast-container";
      toastContainer.setAttribute("aria-live", "polite");
      document.body.appendChild(toastContainer);
    }
    return toastContainer;
  }

  function showToast(type, title, message, duration = 4000, loginUrl = null) {
    const container = getToastContainer();
    const toast = document.createElement("div");
    toast.className = "morong-export-toast toast-" + type;

    let iconSvg = "";
    if (type === "info") {
      iconSvg = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#0284c7" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>';
    } else if (type === "success") {
      iconSvg = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#16a34a" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>';
    } else if (type === "warning") {
      iconSvg = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#eab308" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>';
    } else {
      iconSvg = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';
    }

    toast.innerHTML = `
      <div class="morong-export-toast-icon">${iconSvg}</div>
      <div class="morong-export-toast-body">
        <div class="morong-export-toast-title"></div>
        <div class="morong-export-toast-desc"></div>
      </div>
      <button type="button" class="morong-export-toast-close" aria-label="Đóng">&times;</button>
    `;

    toast.querySelector(".morong-export-toast-title").textContent = title;
    const description = toast.querySelector(".morong-export-toast-desc");
    description.textContent = message;
    if (loginUrl) {
      const login = document.createElement("a");
      login.href = loginUrl; login.textContent = "Đăng nhập lại";
      description.appendChild(document.createTextNode(" ")); description.appendChild(login);
    }
    const closeBtn = toast.querySelector(".morong-export-toast-close");
    let timer = null;

    function dismiss() {
      if (timer) clearTimeout(timer);
      toast.style.opacity = "0";
      toast.style.transform = "translateX(20px)";
      setTimeout(() => {
        if (toast.parentNode) toast.parentNode.removeChild(toast);
      }, 200);
    }

    closeBtn.addEventListener("click", dismiss);
    if (duration > 0) {
      timer = setTimeout(dismiss, duration);
    }

    container.appendChild(toast);
    return { dismiss, update: (newType, newTitle, newMsg, newDuration) => {
      dismiss();
      return showToast(newType, newTitle, newMsg, newDuration);
    }};
  }

  // Khởi tạo các Dropdown Xuất trên trang
  const exportWraps = document.querySelectorAll(".morong-export-wrap");
  if (exportWraps.length === 0) return;

  exportWraps.forEach(function (wrap) {
    const btn = wrap.querySelector(".morong-export-btn");
    const dropdown = wrap.querySelector(".morong-export-dropdown");
    if (!btn || !dropdown) return;

    function openDropdown() {
      // Đóng các dropdown khác nếu có
      exportWraps.forEach(w => {
        if (w !== wrap) {
          w.classList.remove("is-open");
          const otherBtn = w.querySelector(".morong-export-btn");
          if (otherBtn) otherBtn.setAttribute("aria-expanded", "false");
        }
      });
      wrap.classList.add("is-open");
      btn.setAttribute("aria-expanded", "true");
    }

    function closeDropdown(refocus = false) {
      wrap.classList.remove("is-open");
      btn.setAttribute("aria-expanded", "false");
      if (refocus && typeof btn.focus === "function") {
        btn.focus();
      }
    }

    function toggleDropdown() {
      if (wrap.classList.contains("is-open")) {
        closeDropdown(false);
      } else {
        openDropdown();
      }
    }

    btn.addEventListener("click", function (e) {
      e.stopPropagation();
      if (btn.disabled) return;
      toggleDropdown();
    });

    // Keyboard support: Escape & Arrow Navigation
    wrap.addEventListener("keydown", function (e) {
      if (!wrap.classList.contains("is-open")) return;

      if (e.key === "Escape") {
        e.preventDefault();
        closeDropdown(true);
        return;
      }

      if (e.key === "ArrowDown" || e.key === "ArrowUp") {
        const items = Array.from(dropdown.querySelectorAll(".morong-export-item"));
        if (items.length === 0) return;
        e.preventDefault();
        const activeIdx = items.indexOf(document.activeElement);

        if (e.key === "ArrowDown") {
          if (activeIdx < 0 || activeIdx === items.length - 1) {
            items[0].focus();
          } else {
            items[activeIdx + 1].focus();
          }
        } else if (e.key === "ArrowUp") {
          if (activeIdx <= 0) {
            items[items.length - 1].focus();
          } else {
            items[activeIdx - 1].focus();
          }
        }
      }
    });

    // Xử lý tải file qua Fetch khi nhấn vào từng lựa chọn
    const items = dropdown.querySelectorAll(".morong-export-item");
    items.forEach(function (item) {
      item.addEventListener("click", async function (e) {
        const url = item.getAttribute("href");
        if (!url || url === "#") return;

        e.preventDefault();
        if (btn.disabled) return;
        closeDropdown(false);

        const formatLabel = item.getAttribute("data-export-label") || "file";
        const formatExt = item.getAttribute("data-export-format") || "file";

        // Hiển thị trạng thái đang chuẩn bị tải
        const progressToast = showToast(
          "info",
          "Đang xuất dữ liệu...",
          `Hệ thống đang tổng hợp và tạo file ${formatLabel} (${formatExt}), vui lòng đợi trong giây lát.`,
          0 // Không tự tắt
        );

        // Vô hiệu hóa nút tạm thời để tránh bấm liên tục
        btn.disabled = true;
        btn.classList.add("disabled");

        try {
          const response = await fetch(url, {
            method: "GET",
            headers: {
              "Accept": formatExt === "xlsx"
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/octet-stream"
                : "application/pdf, application/octet-stream"
            },
            credentials: "same-origin"
          });

          const expected = new URL(url, window.location.href);
          const finalUrl = new URL(response.url, window.location.href);
          const prefix = expected.pathname.slice(0, expected.pathname.indexOf("/morong/xuat/"));
          const loginUrl = new URL(prefix + "/login", window.location.href);
          const expired = response.status === 401 || (finalUrl.origin === loginUrl.origin && finalUrl.pathname === loginUrl.pathname);
          if (expired) {
            progressToast.dismiss();
            showToast("warning", "Phiên đăng nhập đã hết hạn", "Vui lòng đăng nhập lại rồi thực hiện xuất file.", 0, loginUrl.href);
            return;
          }
          if (response.redirected || !response.url || expected.origin !== window.location.origin
              || finalUrl.origin !== expected.origin || finalUrl.pathname !== expected.pathname) {
            progressToast.dismiss();
            showToast("error", "Không thể tải file", "Máy chủ chuyển đến một trang khác. Vui lòng tải lại trang và thử lại.", 6000);
            return;
          }
          if (response.status !== 200) {
            progressToast.dismiss();
            // Do not echo provider/proxy JSON or HTML: it may contain SQL, stack traces or user input.
            const errors = {
              400: ["Bộ lọc xuất không hợp lệ", "Vui lòng kiểm tra lại điều kiện lọc."],
              403: ["Không có quyền xuất dữ liệu", "Bạn không được phép xuất dữ liệu này."],
              404: ["Không tìm thấy dữ liệu", "Đợt hoặc đường dẫn xuất không tồn tại."],
              422: ["Vượt quá giới hạn xuất dữ liệu", "Excel tối đa 10.000 dòng, PDF tối đa 2.000 dòng. Vui lòng thu hẹp bộ lọc."],
              503: ["Dịch vụ tạm thời gián đoạn", "Chưa thể tạo file. Vui lòng thử lại sau."]
            };
            const error = errors[response.status] || ["Không thể tải file", "Máy chủ chưa trả về file hoàn chỉnh. Vui lòng thử lại sau."];
            showToast(response.status === 422 ? "warning" : "error", error[0], error[1], 6000);
            return;
          }
          const mime = response.headers.get("content-type")?.split(";", 1)[0].trim().toLowerCase();
          const requiredMime = formatExt === "xlsx" ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            : formatExt === "pdf" ? "application/pdf" : null;
          const disposition = response.headers.get("content-disposition") || "";
          const encodedName = disposition.match(/(?:^|;)\s*filename\*=UTF-8''([^;]+)/i);
          const plainName = disposition.match(/(?:^|;)\s*filename=(?:"([^"\r\n]*)"|([^;\r\n]+))/i);
          let filename = encodedName ? decodeURIComponent(encodedName[1].trim()) : plainName ? (plainName[1] ?? plainName[2].trim()) : "";
          if (!requiredMime || mime !== requiredMime || !/^attachment(?:\s*;|\s*$)/i.test(disposition)
              || !/^[^\x00-\x1f\x7f\\/]{1,200}$/.test(filename) || !filename.toLowerCase().endsWith("." + formatExt)) {
            progressToast.dismiss();
            showToast("error", "Phản hồi không phải file hợp lệ", "Máy chủ chưa trả về file Excel/PDF đúng định dạng. Vui lòng tải lại trang.", 6000);
            return;
          }
          // Verify the body before creating a Blob; reject HTML/empty/truncated files even with misleading headers.
          const bytes = new Uint8Array(await response.arrayBuffer());
          const signature = formatExt === "pdf" ? [37, 80, 68, 70, 45] : [80, 75, 3, 4];
          const length = response.headers.get("content-length");
          const encoding = response.headers.get("content-encoding");
          if (bytes.length <= signature.length || !signature.every((value, index) => bytes[index] === value)
              || (length && (!encoding || encoding === "identity") && Number(length) !== bytes.length)) {
            progressToast.dismiss();
            showToast("error", "File tải về không hợp lệ", "File rỗng, thiếu dữ liệu hoặc sai định dạng. Vui lòng thử lại sau.", 6000);
            return;
          }
          const blob = new Blob([bytes], { type: requiredMime });
          const downloadUrl = window.URL.createObjectURL(blob);
          const a = document.createElement("a");
          a.style.display = "none"; a.href = downloadUrl; a.download = filename;
          document.body.appendChild(a); a.click();
          setTimeout(function () {
            window.URL.revokeObjectURL(downloadUrl);
            if (a.parentNode) a.parentNode.removeChild(a);
          }, 500);
          progressToast.dismiss();
          showToast("success", "Đã chuẩn bị file tải xuống", `File ${filename} đã được chuyển cho trình duyệt để tải xuống.`, 4000);
        } catch (netErr) {
          progressToast.dismiss();
          showToast(
            "error",
            "Lỗi kết nối",
            "Không thể kết nối tới máy chủ. Vui lòng kiểm tra đường truyền mạng hoặc thử lại sau.",
            6000
          );
        } finally {
          btn.disabled = false;
          btn.classList.remove("disabled");
        }
      });
    });
  });

  // Đóng dropdown khi click bên ngoài
  document.addEventListener("click", function (e) {
    exportWraps.forEach(function (wrap) {
      if (wrap.classList.contains("is-open") && !wrap.contains(e.target)) {
        wrap.classList.remove("is-open");
        const btn = wrap.querySelector(".morong-export-btn");
        if (btn) btn.setAttribute("aria-expanded", "false");
      }
    });
  });
});
