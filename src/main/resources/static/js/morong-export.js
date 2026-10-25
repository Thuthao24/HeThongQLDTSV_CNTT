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

  function showToast(type, title, message, duration = 4000) {
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
        <div class="morong-export-toast-title">${title}</div>
        <div class="morong-export-toast-desc">${message}</div>
      </div>
      <button type="button" class="morong-export-toast-close" aria-label="Đóng">&times;</button>
    `;

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

          // Trường hợp Thành công 200
          if (response.ok) {
            const contentType = response.headers.get("content-type") || "";
            // Đảm bảo không phải JSON lỗi
            if (contentType.includes("application/json")) {
              const errJson = await response.json().catch(() => ({}));
              progressToast.dismiss();
              showToast(
                "error",
                "Lỗi định dạng phản hồi",
                errJson.message || "Máy chủ trả về dữ liệu không đúng định dạng file.",
                6000
              );
              return;
            }

            // Lấy tên file từ header Content-Disposition
            let filename = `xuat_dulieu.${formatExt}`;
            const disposition = response.headers.get("content-disposition");
            if (disposition && disposition.includes("filename=")) {
              const matches = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
              if (matches && matches[1]) {
                filename = matches[1].replace(/['"]/g, "").trim();
              }
            }

            const blob = await response.blob();
            const downloadUrl = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.style.display = "none";
            a.href = downloadUrl;
            a.download = filename;
            document.body.appendChild(a);
            a.click();

            setTimeout(function () {
              window.URL.revokeObjectURL(downloadUrl);
              if (a.parentNode) a.parentNode.removeChild(a);
            }, 500);

            progressToast.dismiss();
            showToast("success", "Tải file thành công", `File ${filename} đã được tải về máy của bạn.`, 4000);
          } else {
            // Xử lý các mã lỗi HTTP cụ thể theo hợp đồng backend
            progressToast.dismiss();
            let errorMsg = "";

            try {
              const errData = await response.json();
              if (errData && errData.message) {
                errorMsg = errData.message;
              }
            } catch (e) {
              // Phản hồi không phải JSON
            }

            if (response.status === 422) {
              // HTTP 422: Vượt quá giới hạn xuất dữ liệu (ExportLimitException)
              showToast(
                "warning",
                "Vượt quá giới hạn xuất dữ liệu (422)",
                errorMsg || "Dữ liệu xuất vượt quá giới hạn tối đa của hệ thống (Excel tối đa 10.000 dòng, PDF tối đa 2.000 dòng). Vui lòng thu hẹp bộ lọc tìm kiếm và thử lại.",
                8000
              );
            } else if (response.status === 400) {
              // HTTP 400: Bộ lọc không hợp lệ
              showToast(
                "error",
                "Bộ lọc xuất không hợp lệ (400)",
                errorMsg || "Tham số hoặc điều kiện lọc không hợp lệ. Vui lòng kiểm tra lại bộ lọc.",
                6000
              );
            } else if (response.status === 403) {
              // HTTP 403: Không có quyền
              showToast(
                "error",
                "Không có quyền xuất dữ liệu (403)",
                errorMsg || "Bạn không được phép thực hiện chức năng xuất dữ liệu này.",
                6000
              );
            } else if (response.status === 404) {
              // HTTP 404: Không tìm thấy dữ liệu/đợt
              showToast(
                "error",
                "Dữ liệu không tồn tại (404)",
                errorMsg || "Đợt yêu cầu hoặc đường dẫn xuất không tìm thấy trên hệ thống.",
                6000
              );
            } else if (response.status === 503) {
              // HTTP 503: Lỗi dịch vụ backend hoặc tạo file
              showToast(
                "error",
                "Lỗi hệ thống tạm thời (503)",
                errorMsg || "Máy chủ không thể tạo file lúc này. Vui lòng thử lại sau.",
                6000
              );
            } else {
              showToast(
                "error",
                `Lỗi tải file (${response.status})`,
                errorMsg || "Không thể tải file báo cáo từ máy chủ.",
                6000
              );
            }
          }
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
