/**
 * ==========================================================================
 * MODULE 17: GIÁM SÁT EMAIL THÔNG BÁO (PORTAL HCMUTE)
 * File: morong-email-thongbao.js
 * Kết nối JSON API: GET /morong/email-thongbao
 * ==========================================================================
 */

document.addEventListener("DOMContentLoaded", function () {
  // Trạng thái phân trang và bộ lọc nội bộ
  let currentPage = 0;
  let pageSize = 20;
  let activeRequestId = 0;
  let currentItems = [];

  // DOM Elements - Bộ lọc
  const filterForm = document.getElementById("emailFilterForm");
  const thongbaoIdInput = document.getElementById("filterThongbaoId");
  const thongbaoIdError = document.getElementById("filterThongbaoIdError");
  const trangthaiSelect = document.getElementById("filterTrangthai");
  const pageSizeSelect = document.getElementById("filterPageSize");
  const btnResetFilter = document.getElementById("btnResetFilter");
  const btnRefresh = document.getElementById("btnRefresh");
  const btnRetryFetch = document.getElementById("btnRetryFetch");

  // DOM Elements - Trạng thái hệ thống
  const systemStatusSection = document.getElementById("systemStatusSection");
  const schemaAlert = document.getElementById("schemaUnavailableAlert");
  const flagOutboxDot = document.getElementById("flagOutboxDot");
  const flagOutboxText = document.getElementById("flagOutboxText");
  const flagDeliveryDot = document.getElementById("flagDeliveryDot");
  const flagDeliveryText = document.getElementById("flagDeliveryText");

  // DOM Elements - Thống kê KPI
  const kpiTotal = document.getElementById("kpiTotal");
  const kpiPending = document.getElementById("kpiPending");
  const kpiSending = document.getElementById("kpiSending");
  const kpiSent = document.getElementById("kpiSent");
  const kpiFailed = document.getElementById("kpiFailed");
  const kpiMissingEmail = document.getElementById("kpiMissingEmail");
  const kpiUncertain = document.getElementById("kpiUncertain");
  const cardKpiSending = document.getElementById("cardKpiSending");
  const cardKpiMissing = document.getElementById("cardKpiMissing");
  const cardKpiUncertain = document.getElementById("cardKpiUncertain");

  // DOM Elements - Bảng dữ liệu
  const tableCard = document.querySelector(".email-table-card");
  const emailTable = document.getElementById("emailDataTable");
  const tableBody = document.getElementById("emailTableBody");
  const totalElementsBold = document.getElementById("totalElementsBold");
  const loadingIndicator = document.getElementById("tableLoadingIndicator");
  const emptyState = document.getElementById("emailEmptyState");
  const emptyTitle = document.getElementById("emptyStateTitle");
  const emptyDesc = document.getElementById("emptyStateDesc");
  const errorState = document.getElementById("emailErrorState");
  const errorTitle = document.getElementById("errorStateTitle");
  const errorDesc = document.getElementById("errorStateDesc");

  // DOM Elements - Phân trang
  const pageCurrentInfo = document.getElementById("pageCurrentInfo");
  const pageSummaryCount = document.getElementById("pageSummaryCount");
  const btnPrevPage = document.getElementById("btnPrevPage");
  const btnNextPage = document.getElementById("btnNextPage");

  // DOM Elements - Modal chi tiết
  const detailModal = document.getElementById("emailDetailModal");
  const modalCloseBtn = document.getElementById("modalCloseBtn");
  const modalCloseFooterBtn = document.getElementById("modalCloseFooterBtn");
  const modalStatusBadge = document.getElementById("modalStatusBadge");
  const modalRecipient = document.getElementById("modalRecipient");
  const modalEmailAddress = document.getElementById("modalEmailAddress");
  const modalNoticeTitle = document.getElementById("modalNoticeTitle");
  const modalNoticeLinkWrap = document.getElementById("modalNoticeLinkWrap");
  const modalCreatedAt = document.getElementById("modalCreatedAt");
  const modalCreatedAtItem = document.getElementById("modalCreatedAtItem");
  const modalCreatedAtLabel = document.getElementById("modalCreatedAtLabel");
  const modalSentAt = document.getElementById("modalSentAt");
  const modalSentAtItem = document.getElementById("modalSentAtItem");
  const modalSentAtLabel = document.getElementById("modalSentAtLabel");
  const modalErrorSection = document.getElementById("modalErrorSection");
  const modalErrorMessage = document.getElementById("modalErrorMessage");

  // ===== Định dạng hiển thị ngày giờ (dd/MM/yyyy HH:mm) =====
  function formatDateTime(isoString) {
    if (!isoString) return "—";
    if (typeof isoString === "string") {
      const parts = isoString.split("T");
      if (parts.length >= 2) {
        const dateParts = parts[0].split("-");
        const timeParts = parts[1].split(":");
        if (dateParts.length === 3 && timeParts.length >= 2) {
          const y = dateParts[0];
          const m = dateParts[1];
          const d = dateParts[2];
          const hr = timeParts[0];
          const min = timeParts[1];
          return `${d}/${m}/${y} ${hr}:${min}`;
        }
      }
    }
    try {
      const date = new Date(isoString);
      if (isNaN(date.getTime())) return String(isoString);
      const pad = (n) => (n < 10 ? "0" + n : String(n));
      return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    } catch (e) {
      return String(isoString);
    }
  }

  // Định dạng số phân cách hàng nghìn
  function formatNumber(num) {
    if (num === null || num === undefined) return "0";
    return Number(num).toLocaleString("vi-VN");
  }

  // Thông tin cấu hình huy hiệu trạng thái
  const STATUS_CONFIG = {
    PENDING: { label: "Chờ gửi", className: "badge-pending" },
    SENDING: { label: "Đang gửi", className: "badge-sending" },
    SENT: { label: "Đã gửi", className: "badge-sent" },
    FAILED: { label: "Thất bại", className: "badge-failed" },
    MISSING_EMAIL: { label: "Thiếu email", className: "badge-missing" },
    UNCERTAIN: { label: "Chưa xác định", className: "badge-uncertain" }
  };

  function createStatusBadge(statusKey) {
    const span = document.createElement("span");
    const cfg = STATUS_CONFIG[statusKey] || { label: statusKey || "—", className: "badge-missing" };
    span.className = "status-badge " + cfg.className;
    span.textContent = cfg.label;
    return span;
  }

  // Xóa nội dung một node an toàn
  function clearNode(node) {
    while (node && node.firstChild) {
      node.removeChild(node.firstChild);
    }
  }

  // Hiển thị trạng thái lỗi
  function showError(title, message) {
    if (loadingIndicator) loadingIndicator.style.display = "none";
    if (emailTable) emailTable.style.display = "none";
    if (emptyState) emptyState.style.display = "none";
    if (errorState) {
      errorState.style.display = "block";
      if (errorTitle) errorTitle.textContent = title;
      if (errorDesc) errorDesc.textContent = message;
    }
  }

  // Hiển thị trạng thái trống
  function showEmpty(title, message) {
    if (loadingIndicator) loadingIndicator.style.display = "none";
    if (emailTable) emailTable.style.display = "none";
    if (errorState) errorState.style.display = "none";
    if (emptyState) {
      emptyState.style.display = "block";
      if (emptyTitle) emptyTitle.textContent = title;
      if (emptyDesc) emptyDesc.textContent = message;
    }
  }

  // Hiển thị bảng dữ liệu
  function showTable() {
    if (loadingIndicator) loadingIndicator.style.display = "none";
    if (errorState) errorState.style.display = "none";
    if (emptyState) emptyState.style.display = "none";
    if (emailTable) emailTable.style.display = "table";
  }

  // ===== Tải dữ liệu từ API Backend =====
  async function loadEmailData() {
    const requestId = ++activeRequestId;

    // Xác thực input ID thông báo phía client
    const rawThongbaoId = thongbaoIdInput.value.trim();
    if (rawThongbaoId) {
      const num = Number(rawThongbaoId);
      if (!Number.isInteger(num) || num <= 0) {
        thongbaoIdInput.classList.add("is-invalid");
        if (thongbaoIdError) {
          thongbaoIdError.textContent = "ID thông báo phải là số nguyên dương hợp lệ.";
          thongbaoIdError.style.display = "block";
        }
        return;
      }
    }
    thongbaoIdInput.classList.remove("is-invalid");
    if (thongbaoIdError) thongbaoIdError.style.display = "none";

    // Hiển thị loading
    if (loadingIndicator) loadingIndicator.style.display = "inline-flex";

    // Chuẩn bị URL và tham số query
    const params = new URLSearchParams();
    if (rawThongbaoId) {
      params.append("thongbaoid", rawThongbaoId);
    }
    const selectedTrangthai = trangthaiSelect.value;
    if (selectedTrangthai) {
      params.append("trangthai", selectedTrangthai);
    }
    params.append("page", String(currentPage));
    params.append("size", String(pageSize));

    const apiUrl = "/morong/email-thongbao?" + params.toString();

    try {
      const response = await fetch(apiUrl, {
        method: "GET",
        headers: {
          "Accept": "application/json"
        },
        credentials: "same-origin"
      });

      // Nếu yêu cầu cũ quay về trễ hơn yêu cầu mới -> bỏ qua để tránh race condition
      if (requestId !== activeRequestId) return;

      // Kiểm tra chuyển hướng HTML /login (khi hết phiên)
      const contentType = response.headers.get("content-type") || "";
      if (contentType.includes("text/html")) {
        showError(
          "Phiên làm việc đã hết hạn",
          "Hệ thống đang chuyển hướng tới trang đăng nhập, vui lòng đợi..."
        );
        setTimeout(function () {
          window.location.href = "/login";
        }, 1500);
        return;
      }

      if (response.ok) {
        const data = await response.json();
        renderMonitorData(data);
      } else {
        let errMsg = "Không thể tải dữ liệu email từ máy chủ.";
        try {
          const errData = await response.json();
          if (errData && errData.message) {
            errMsg = errData.message;
          }
        } catch (e) {}

        if (response.status === 400) {
          showError("Tham số lọc không hợp lệ (400)", errMsg);
        } else if (response.status === 403) {
          showError("Không có quyền truy cập (403)", "Chức năng Giám sát Email thông báo chỉ dành riêng cho Trưởng khoa.");
        } else if (response.status === 503) {
          showError("Dịch vụ tạm thời gián đoạn (503)", errMsg || "Chưa thể kết nối tới cơ sở dữ liệu hàng đợi email.");
        } else {
          showError(`Lỗi máy chủ (${response.status})`, errMsg);
        }
      }
    } catch (networkErr) {
      if (requestId !== activeRequestId) return;
      showError(
        "Lỗi kết nối",
        "Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại đường truyền mạng hoặc khởi động lại ứng dụng."
      );
    } finally {
      if (requestId === activeRequestId && loadingIndicator) {
        loadingIndicator.style.display = "none";
      }
    }
  }

  // ===== Kết xuất dữ liệu Monitor lên giao diện =====
  function renderMonitorData(data) {
    if (!data) return;

    // 1. Trạng thái hệ thống (Banners & Flags)
    if (schemaAlert) {
      if (data.schemaAvailable === false) {
        schemaAlert.style.display = "flex";
        if (systemStatusSection) systemStatusSection.style.display = "block";
      } else {
        schemaAlert.style.display = "none";
        if (systemStatusSection) systemStatusSection.style.display = "none";
      }
    }

    if (flagOutboxDot && flagOutboxText) {
      if (data.outboxEnabled) {
        flagOutboxDot.className = "flag-indicator active";
        flagOutboxText.textContent = "Đang bật";
      } else {
        flagOutboxDot.className = "flag-indicator inactive";
        flagOutboxText.textContent = "Chức năng tạo hàng đợi email đang tắt";
      }
    }

    if (flagDeliveryDot && flagDeliveryText) {
      if (data.deliveryEnabled) {
        flagDeliveryDot.className = "flag-indicator active";
        flagDeliveryText.textContent = "Đang bật";
      } else {
        flagDeliveryDot.className = "flag-indicator inactive";
        flagDeliveryText.textContent = "Chức năng gửi email SMTP hiện đang tắt";
      }
    }

    // 2. Thống kê KPI (Counts)
    const counts = data.counts || {};
    if (kpiTotal) kpiTotal.textContent = formatNumber(counts.total);
    if (kpiPending) kpiPending.textContent = formatNumber(counts.pending);
    if (kpiSent) kpiSent.textContent = formatNumber(counts.sent);
    if (kpiFailed) kpiFailed.textContent = formatNumber(counts.failed);
    if (kpiSending) kpiSending.textContent = formatNumber(counts.sending);
    if (kpiMissingEmail) kpiMissingEmail.textContent = formatNumber(counts.missingEmail);
    if (kpiUncertain) kpiUncertain.textContent = formatNumber(counts.uncertain);

    // Chỉ hiển thị các thẻ KPI phụ khi số lượng > 0 để màn hình chính gọn gàng
    const sendingCount = Number(counts.sending) || 0;
    const missingCount = Number(counts.missingEmail) || 0;
    const uncertainCount = Number(counts.uncertain) || 0;

    if (cardKpiSending) cardKpiSending.style.display = sendingCount > 0 ? "flex" : "none";
    if (cardKpiMissing) cardKpiMissing.style.display = missingCount > 0 ? "flex" : "none";
    if (cardKpiUncertain) cardKpiUncertain.style.display = uncertainCount > 0 ? "flex" : "none";

    // 3. Bảng danh sách email
    currentItems = Array.isArray(data.items) ? data.items : [];
    clearNode(tableBody);

    if (currentItems.length === 0) {
      if (data.schemaAvailable === false) {
        showEmpty(
          "Chưa triển khai schema",
          "Chưa triển khai bảng lưu hàng đợi Email thông báo trong cơ sở dữ liệu."
        );
      } else if (thongbaoIdInput.value.trim() || trangthaiSelect.value) {
        showEmpty(
          "Không tìm thấy email",
          "Không có email nào khớp với điều kiện lọc hiện tại."
        );
      } else {
        showEmpty(
          "Hàng đợi email đang trống",
          "Hệ thống chưa ghi nhận email thông báo nào trong hàng đợi."
        );
      }
    } else {
      showTable();

      currentItems.forEach(function (item) {
        const tr = document.createElement("tr");

        // Cột 1: ID
        const tdId = document.createElement("td");
        tdId.textContent = "#" + (item.id !== null ? item.id : "—");
        tr.appendChild(tdId);

        // Cột 2: Người nhận
        const tdRecipient = document.createElement("td");
        tdRecipient.className = "email-recipient-cell";
        tdRecipient.textContent = item.nguoinhan || "—";
        tr.appendChild(tdRecipient);

        // Cột 3: Địa chỉ Email
        const tdEmail = document.createElement("td");
        if (item.email) {
          tdEmail.className = "email-address-cell";
          tdEmail.textContent = item.email;
        } else {
          const spanMissing = document.createElement("span");
          spanMissing.className = "email-address-missing";
          spanMissing.textContent = "Chưa có email";
          tdEmail.appendChild(spanMissing);
        }
        tr.appendChild(tdEmail);

        // Cột 4: Tiêu đề thông báo
        const tdTitle = document.createElement("td");
        tdTitle.className = "email-title-cell";
        tdTitle.title = item.tieude || "";
        tdTitle.textContent = item.tieude || "—";
        tr.appendChild(tdTitle);

        // Cột 5: Trạng thái
        const tdStatus = document.createElement("td");
        tdStatus.style.textAlign = "center";
        tdStatus.appendChild(createStatusBadge(item.trangthai));
        tr.appendChild(tdStatus);

        // Cột 6: Ngày tạo
        const tdCreated = document.createElement("td");
        tdCreated.textContent = formatDateTime(item.ngaytao);
        tr.appendChild(tdCreated);

        // Cột 7: Ngày gửi
        const tdSent = document.createElement("td");
        tdSent.textContent = item.ngaygui ? formatDateTime(item.ngaygui) : "—";
        tr.appendChild(tdSent);

        // Cột 8: Thao tác (Chi tiết)
        const tdAction = document.createElement("td");
        tdAction.style.textAlign = "center";
        const btnView = document.createElement("button");
        btnView.type = "button";
        btnView.className = "btn-detail";
        btnView.textContent = "Chi tiết";
        btnView.setAttribute("aria-label", "Xem chi tiết email #" + item.id);
        btnView.addEventListener("click", function () {
          openDetailModal(item);
        });
        tdAction.appendChild(btnView);
        tr.appendChild(tdAction);

        tableBody.appendChild(tr);
      });
    }

    // 4. Phân trang
    const pageInfo = data.pageInfo || { number: 0, size: pageSize, totalElements: 0, totalPages: 0 };
    if (totalElementsBold) {
      totalElementsBold.textContent = formatNumber(pageInfo.totalElements);
    }

    if (pageInfo.totalPages === 0) {
      if (pageCurrentInfo) pageCurrentInfo.textContent = "Trang 0 / 0";
      if (btnPrevPage) btnPrevPage.disabled = true;
      if (btnNextPage) btnNextPage.disabled = true;
    } else {
      if (pageCurrentInfo) {
        pageCurrentInfo.textContent = `Trang ${pageInfo.number + 1} / ${pageInfo.totalPages}`;
      }
      if (btnPrevPage) {
        btnPrevPage.disabled = pageInfo.number <= 0;
      }
      if (btnNextPage) {
        btnNextPage.disabled = pageInfo.number >= pageInfo.totalPages - 1;
      }
    }

    if (pageSummaryCount) {
      pageSummaryCount.textContent = `Tổng số ${formatNumber(pageInfo.totalElements)} email`;
    }
  }

  // ===== Modal xem chi tiết Email =====
  function openDetailModal(item) {
    if (!item || !detailModal) return;

    if (modalStatusBadge) {
      clearNode(modalStatusBadge);
      modalStatusBadge.appendChild(createStatusBadge(item.trangthai));
    }

    if (modalRecipient) modalRecipient.textContent = item.nguoinhan || "—";

    if (modalEmailAddress) {
      if (item.email) {
        modalEmailAddress.textContent = item.email;
        modalEmailAddress.classList.remove("email-address-missing");
      } else {
        modalEmailAddress.textContent = "Chưa có email";
        modalEmailAddress.classList.add("email-address-missing");
      }
    }

    if (modalNoticeTitle) modalNoticeTitle.textContent = item.tieude || "—";

    // Thông báo gốc
    if (modalNoticeLinkWrap) {
      clearNode(modalNoticeLinkWrap);
      if (item.thongbaoid !== null && item.thongbaoid !== undefined) {
        const link = document.createElement("a");
        link.href = "/thongbao/" + encodeURIComponent(item.thongbaoid);
        link.target = "_blank";
        link.rel = "noopener noreferrer";
        link.className = "detail-link";
        link.textContent = "Xem thông báo ↗";
        modalNoticeLinkWrap.appendChild(link);
      } else {
        const deletedSpan = document.createElement("span");
        deletedSpan.className = "badge-deleted";
        deletedSpan.textContent = "Thông báo gốc đã bị xóa.";
        modalNoticeLinkWrap.appendChild(deletedSpan);
      }
    }

    // Xử lý các mốc thời gian: với email SENT, không hiển thị hai mốc thời gian trùng nhau
    const createdStr = formatDateTime(item.ngaytao);
    const sentStr = item.ngaygui ? formatDateTime(item.ngaygui) : "";

    if (modalCreatedAt) modalCreatedAt.textContent = createdStr;
    if (modalSentAt) modalSentAt.textContent = sentStr || "Chưa gửi";

    if (item.trangthai === "SENT") {
      if (sentStr && createdStr === sentStr) {
        // Ngày tạo và ngày gửi cùng mốc: chỉ hiển thị 1 mốc thời gian gửi
        if (modalCreatedAtItem) modalCreatedAtItem.style.display = "none";
        if (modalSentAtLabel) modalSentAtLabel.textContent = "Thời gian gửi:";
        if (modalSentAt) modalSentAt.textContent = `${sentStr} (gửi ngay lúc tạo)`;
      } else {
        if (modalCreatedAtItem) modalCreatedAtItem.style.display = "flex";
        if (modalCreatedAtLabel) modalCreatedAtLabel.textContent = "Ngày tạo:";
        if (modalSentAtLabel) modalSentAtLabel.textContent = "Ngày gửi:";
      }
    } else {
      if (modalCreatedAtItem) modalCreatedAtItem.style.display = "flex";
      if (modalCreatedAtLabel) modalCreatedAtLabel.textContent = "Ngày tạo:";
      if (modalSentAtLabel) modalSentAtLabel.textContent = "Ngày gửi:";
    }

    // Chi tiết lỗi khi gửi thất bại (FAILED / UNCERTAIN hoặc có thông báo lỗi)
    if (modalErrorSection && modalErrorMessage) {
      const hasError = (item.trangthai === "FAILED" || item.trangthai === "UNCERTAIN" || (item.loi && item.loi.trim()));
      if (hasError) {
        modalErrorSection.style.display = "block";
        modalErrorMessage.textContent = (item.loi && item.loi.trim())
          ? item.loi
          : "Quá trình gửi thư phát sinh sự cố hoặc chưa nhận được phản hồi từ máy chủ SMTP.";
      } else {
        modalErrorSection.style.display = "none";
        modalErrorMessage.textContent = "";
      }
    }

    detailModal.style.display = "flex";
    if (modalCloseBtn) modalCloseBtn.focus();
  }

  function closeDetailModal() {
    if (detailModal) {
      detailModal.style.display = "none";
    }
  }

  if (modalCloseBtn) modalCloseBtn.addEventListener("click", closeDetailModal);
  if (modalCloseFooterBtn) modalCloseFooterBtn.addEventListener("click", closeDetailModal);

  // Đóng modal khi click nền hoặc phím Escape
  if (detailModal) {
    detailModal.addEventListener("click", function (e) {
      if (e.target === detailModal) {
        closeDetailModal();
      }
    });
  }

  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape" && detailModal && detailModal.style.display === "flex") {
      closeDetailModal();
    }
  });

  // ===== Xử lý Form Bộ lọc & Nút bấm =====
  if (filterForm) {
    filterForm.addEventListener("submit", function (e) {
      e.preventDefault();
      currentPage = 0; // Luôn quay về trang đầu khi lọc
      loadEmailData();
    });
  }

  if (btnResetFilter) {
    btnResetFilter.addEventListener("click", function () {
      if (thongbaoIdInput) thongbaoIdInput.value = "";
      if (trangthaiSelect) trangthaiSelect.value = "";
      if (pageSizeSelect) pageSizeSelect.value = "20";
      if (thongbaoIdInput) thongbaoIdInput.classList.remove("is-invalid");
      if (thongbaoIdError) thongbaoIdError.style.display = "none";
      currentPage = 0;
      pageSize = 20;
      loadEmailData();
    });
  }

  if (pageSizeSelect) {
    pageSizeSelect.addEventListener("change", function () {
      pageSize = Number(pageSizeSelect.value) || 20;
      currentPage = 0;
      loadEmailData();
    });
  }

  if (btnRefresh) {
    btnRefresh.addEventListener("click", function () {
      loadEmailData();
    });
  }

  if (btnRetryFetch) {
    btnRetryFetch.addEventListener("click", function () {
      loadEmailData();
    });
  }

  // Xử lý Phân trang
  if (btnPrevPage) {
    btnPrevPage.addEventListener("click", function () {
      if (currentPage > 0) {
        currentPage--;
        loadEmailData();
      }
    });
  }

  if (btnNextPage) {
    btnNextPage.addEventListener("click", function () {
      currentPage++;
      loadEmailData();
    });
  }

  // Tải dữ liệu ban đầu khi mở trang
  loadEmailData();
});
