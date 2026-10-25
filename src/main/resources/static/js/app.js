document.addEventListener("DOMContentLoaded", function () {
  const toggle = document.getElementById("menuToggle");
  const sidebar = document.getElementById("sidebar");
  if (toggle && sidebar) {
    toggle.addEventListener("click", function () { const opened = sidebar.classList.toggle("open"); toggle.setAttribute("aria-expanded", String(opened)); });
    document.addEventListener("click", function (e) {
      if (window.innerWidth <= 820 && sidebar.classList.contains("open") && !sidebar.contains(e.target) && !toggle.contains(e.target)) { sidebar.classList.remove("open"); toggle.setAttribute("aria-expanded", "false"); }
    });
  }
  const search = document.querySelector("[data-table-search]");
  const tbody = document.querySelector("[data-table-body]");
  if (search && tbody) {
    search.addEventListener("input", function () {
      const q = search.value.trim().toLowerCase();
      tbody.querySelectorAll("tr").forEach(function (row) {
        row.style.display = row.textContent.toLowerCase().includes(q) ? "" : "none";
      });
    });
  }

  // ===== Module 15: Delete Confirmation Modal =====
  function initDeleteModal() {
    const modal = document.getElementById("deleteModal");
    if (!modal) return;

    const titleEl = document.getElementById("deleteModalNoticeTitle");
    const confirmBtn = document.getElementById("deleteModalConfirmBtn");
    const cancelBtn = document.getElementById("deleteModalCancelBtn");
    let activeTargetForm = null;
    let activeTriggerBtn = null;
    let isSubmitting = false;

    function openModal(triggerBtn) {
      const formId = triggerBtn.getAttribute("data-target-form");
      const targetForm = formId ? document.getElementById(formId) : null;
      if (!targetForm) return;

      activeTargetForm = targetForm;
      activeTriggerBtn = triggerBtn;
      isSubmitting = false;

      const noticeTitle = triggerBtn.getAttribute("data-notice-title") || "---";
      if (titleEl) {
        titleEl.textContent = noticeTitle;
      }

      if (confirmBtn) {
        confirmBtn.disabled = false;
        const btnText = confirmBtn.querySelector(".btn-text");
        if (btnText) btnText.textContent = "Xác nhận xóa";
      }
      if (cancelBtn) {
        cancelBtn.disabled = false;
      }

      modal.classList.add("open");
      modal.setAttribute("aria-hidden", "false");
      document.body.classList.add("modal-open");

      // Focus cancel button for safe keyboard interaction
      if (cancelBtn) {
        setTimeout(function () {
          cancelBtn.focus();
        }, 50);
      }
    }

    function closeModal() {
      if (isSubmitting) return;

      modal.classList.remove("open");
      modal.setAttribute("aria-hidden", "true");
      document.body.classList.remove("modal-open");

      activeTargetForm = null;

      if (activeTriggerBtn && typeof activeTriggerBtn.focus === "function") {
        activeTriggerBtn.focus();
      }
      activeTriggerBtn = null;
    }

    document.addEventListener("click", function (e) {
      const trigger = e.target.closest(".btn-trigger-delete");
      if (trigger) {
        e.preventDefault();
        openModal(trigger);
        return;
      }

      if (e.target.closest("[data-modal-dismiss]")) {
        e.preventDefault();
        closeModal();
      }
    });

    if (confirmBtn) {
      confirmBtn.addEventListener("click", function () {
        if (!activeTargetForm || isSubmitting) return;

        isSubmitting = true;
        confirmBtn.disabled = true;
        if (cancelBtn) cancelBtn.disabled = true;

        const btnText = confirmBtn.querySelector(".btn-text");
        if (btnText) btnText.textContent = "Đang xóa...";

        activeTargetForm.submit();
      });
    }

    document.addEventListener("keydown", function (e) {
      if (!modal.classList.contains("open")) return;

      if (e.key === "Escape") {
        e.preventDefault();
        closeModal();
        return;
      }

      if (e.key === "Tab") {
        const focusables = modal.querySelectorAll('button:not([disabled]), [tabindex="0"]');
        if (focusables.length === 0) return;
        const first = focusables[0];
        const last = focusables[focusables.length - 1];

        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    });
  }

  initDeleteModal();

  // ===== Module 17: Namespace morong - Date Picker (dd/MM/yyyy) =====
  function initMorongDatePicker() {
    const wraps = document.querySelectorAll(".morong-date-picker-wrap");
    if (wraps.length === 0) return;

    function parseAndValidateVN(str) {
      if (!str || !str.trim()) return { valid: true, iso: "", formatted: "" };
      const trimmed = str.trim();
      const regex = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/;
      const match = trimmed.match(regex);
      if (!match) {
        return { valid: false, error: "Định dạng ngày phải là dd/MM/yyyy (ví dụ: 06/10/2026)." };
      }
      const d = parseInt(match[1], 10);
      const m = parseInt(match[2], 10);
      const y = parseInt(match[3], 10);

      if (y < 1900 || y > 9998) {
        return { valid: false, error: "Năm phải từ 1900 đến 9998." };
      }
      if (m < 1 || m > 12) {
        return { valid: false, error: "Tháng không hợp lệ (phải từ 01 đến 12)." };
      }
      const isLeap = (y % 4 === 0 && y % 100 !== 0) || (y % 400 === 0);
      const daysInMonth = [0, 31, isLeap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
      const maxDays = daysInMonth[m];

      if (d < 1 || d > maxDays) {
        if (m === 2 && d === 29 && !isLeap) {
          return { valid: false, error: "Năm " + y + " không phải năm nhuận, tháng 2 chỉ có 28 ngày." };
        }
        return { valid: false, error: "Ngày " + d + " không hợp lệ trong tháng " + m + "/" + y + " (tối đa " + maxDays + " ngày)." };
      }

      const pad = function (n) { return n < 10 ? "0" + n : "" + n; };
      const iso = y + "-" + pad(m) + "-" + pad(d);
      const formatted = pad(d) + "/" + pad(m) + "/" + y;
      return { valid: true, iso: iso, formatted: formatted };
    }

    wraps.forEach(function (wrap) {
      const displayInput = wrap.querySelector(".morong-date-display");
      const targetId = displayInput ? displayInput.getAttribute("data-target") : null;
      const hiddenInput = targetId ? document.getElementById(targetId) : null;
      const nativePicker = wrap.querySelector(".morong-date-native-picker");
      const calBtn = wrap.querySelector(".morong-date-picker-btn");
      const parentField = wrap.closest(".morong-filter-field");
      const errEl = parentField ? parentField.querySelector(".morong-date-error") : null;

      if (!displayInput || !hiddenInput) return;

      function showError(msg) {
        displayInput.classList.add("is-invalid");
        if (errEl) {
          errEl.textContent = msg;
          errEl.style.display = "block";
        }
      }

      function clearError() {
        displayInput.classList.remove("is-invalid");
        if (errEl) {
          errEl.textContent = "";
          errEl.style.display = "none";
        }
      }

      // Sync display with hidden on load if hidden has ISO value and display is empty
      if (hiddenInput.value && !displayInput.value) {
        const parts = hiddenInput.value.split("-");
        if (parts.length === 3) {
          displayInput.value = parts[2] + "/" + parts[1] + "/" + parts[0];
        }
      }

      // Auto-insert slash when typing ddMM
      displayInput.addEventListener("input", function () {
        let val = displayInput.value;
        if (/^\d{2}$/.test(val)) {
          displayInput.value = val + "/";
        } else if (/^\d{2}\/\d{2}$/.test(val)) {
          displayInput.value = val + "/";
        }
        clearError();
      });

      function syncFromDisplay() {
        const val = displayInput.value.trim();
        if (!val) {
          hiddenInput.value = "";
          clearError();
          return true;
        }
        const res = parseAndValidateVN(val);
        if (res.valid) {
          hiddenInput.value = res.iso;
          displayInput.value = res.formatted;
          if (nativePicker) nativePicker.value = res.iso;
          clearError();
          return true;
        } else {
          hiddenInput.value = "";
          showError(res.error);
          return false;
        }
      }

      displayInput.addEventListener("blur", syncFromDisplay);
      displayInput.addEventListener("change", syncFromDisplay);

      if (calBtn && nativePicker) {
        calBtn.addEventListener("click", function (e) {
          e.preventDefault();
          if (hiddenInput.value && /^\d{4}-\d{2}-\d{2}$/.test(hiddenInput.value)) {
            nativePicker.value = hiddenInput.value;
          }
          if (typeof nativePicker.showPicker === "function") {
            try {
              nativePicker.showPicker();
            } catch (err) {
              nativePicker.click();
            }
          } else {
            nativePicker.click();
          }
        });

        nativePicker.addEventListener("change", function () {
          if (!nativePicker.value) return;
          const parts = nativePicker.value.split("-");
          if (parts.length === 3) {
            displayInput.value = parts[2] + "/" + parts[1] + "/" + parts[0];
            hiddenInput.value = nativePicker.value;
            clearError();
          }
        });
      }
    });

    // Form submit validation for date fields
    const form = document.getElementById("filterForm");
    if (form) {
      form.addEventListener("submit", function (e) {
        let hasError = false;
        let firstInvalidInput = null;

        wraps.forEach(function (wrap) {
          const displayInput = wrap.querySelector(".morong-date-display");
          const targetId = displayInput ? displayInput.getAttribute("data-target") : null;
          const hiddenInput = targetId ? document.getElementById(targetId) : null;
          const parentField = wrap.closest(".morong-filter-field");
          const errEl = parentField ? parentField.querySelector(".morong-date-error") : null;

          if (!displayInput || !hiddenInput) return;
          const val = displayInput.value.trim();
          if (!val) {
            hiddenInput.value = "";
            return;
          }

          const res = parseAndValidateVN(val);
          if (!res.valid) {
            hasError = true;
            hiddenInput.value = "";
            displayInput.classList.add("is-invalid");
            if (errEl) {
              errEl.textContent = res.error;
              errEl.style.display = "block";
            }
            if (!firstInvalidInput) firstInvalidInput = displayInput;
          } else {
            hiddenInput.value = res.iso;
            displayInput.value = res.formatted;
            displayInput.classList.remove("is-invalid");
            if (errEl) errEl.style.display = "none";
          }
        });

        if (hasError) {
          e.preventDefault();
          if (firstInvalidInput) firstInvalidInput.focus();
        }
      });
    }
  }

  initMorongDatePicker();

  // ===== Module 15: Header Notification Bell Dropdown =====
  function initHeaderNotices() {
    const btn = document.getElementById("headerNoticeBtn");
    const dropdown = document.getElementById("headerNoticeDropdown");
    const wrap = document.getElementById("headerNoticesWrap");
    if (!btn || !dropdown || !wrap) return;

    function openDropdown() {
      dropdown.classList.add("open");
      btn.setAttribute("aria-expanded", "true");
    }

    function closeDropdown(refocusBtn) {
      if (!dropdown.classList.contains("open")) return;
      dropdown.classList.remove("open");
      btn.setAttribute("aria-expanded", "false");
      if (refocusBtn && typeof btn.focus === "function") {
        btn.focus();
      }
    }

    function toggleDropdown() {
      if (dropdown.classList.contains("open")) {
        closeDropdown(false);
      } else {
        openDropdown();
      }
    }

    btn.addEventListener("click", function (e) {
      e.stopPropagation();
      toggleDropdown();
    });

    // Close when clicking outside
    document.addEventListener("click", function (e) {
      if (dropdown.classList.contains("open") && !wrap.contains(e.target)) {
        closeDropdown(false);
      }
    });

    // Keyboard support: Escape closes & refocuses bell; Arrow navigation inside dropdown
    document.addEventListener("keydown", function (e) {
      if (!dropdown.classList.contains("open")) return;

      if (e.key === "Escape") {
        e.preventDefault();
        closeDropdown(true);
        return;
      }

      if (e.key === "ArrowDown" || e.key === "ArrowUp") {
        const focusable = Array.from(dropdown.querySelectorAll('a[href], button:not([disabled])'));
        if (focusable.length === 0) return;
        const currentIndex = focusable.indexOf(document.activeElement);

        if (e.key === "ArrowDown") {
          e.preventDefault();
          if (currentIndex < 0 || currentIndex === focusable.length - 1) {
            focusable[0].focus();
          } else {
            focusable[currentIndex + 1].focus();
          }
        } else if (e.key === "ArrowUp") {
          e.preventDefault();
          if (currentIndex <= 0) {
            focusable[focusable.length - 1].focus();
          } else {
            focusable[currentIndex - 1].focus();
          }
        }
      }
    });
  }

  function initConfirmModal() {
    const modal = document.getElementById('confirmModal');
    if (!modal) return;
    const msgEl = document.getElementById('confirmModalMessage');
    const titleEl = document.getElementById('confirmModalTitle');
    const acceptBtn = document.getElementById('confirmModalAccept');
    let pendingForm = null;

    document.querySelectorAll('form[data-confirm]').forEach(form => {
      form.addEventListener('submit', function (e) {
        e.preventDefault();
        pendingForm = form;
        const msg = form.getAttribute('data-confirm') || 'Bạn có chắc chắn muốn thực hiện thao tác này?';
        const title = form.getAttribute('data-modal-title') || 'Thông báo';
        if (msgEl) msgEl.textContent = msg;
        if (titleEl) titleEl.textContent = title;
        modal.style.display = 'flex';
        modal.setAttribute('aria-hidden', 'false');
      });
    });

    modal.querySelectorAll('[data-modal-cancel]').forEach(btn => {
      btn.addEventListener('click', function () {
        modal.style.display = 'none';
        modal.setAttribute('aria-hidden', 'true');
        pendingForm = null;
      });
    });

    if (acceptBtn) {
      acceptBtn.addEventListener('click', function () {
        if (pendingForm) {
          const formToSubmit = pendingForm;
          pendingForm = null;
          formToSubmit.removeAttribute('data-confirm');
          formToSubmit.submit();
        }
      });
    }
  }

  initConfirmModal();
  initHeaderNotices();
});