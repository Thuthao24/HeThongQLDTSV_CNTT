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

  // ===== Module Nop Bao Cao: Toggle Resubmit & Drag Drop =====
  function initNopBaoCaoModule() {
    const btnToggle = document.getElementById('btnToggleResubmit');
    const btnCancel = document.getElementById('btnCancelResubmit');
    const formContainer = document.getElementById('uploadFormContainer');
    const fileCard = document.getElementById('submittedFileCard');
    const fileInput = document.getElementById('file');
    const fileNameDisplay = document.getElementById('selectedFileName');
    const dropzone = document.getElementById('uploadDropzone');

    if (btnToggle && formContainer) {
      btnToggle.addEventListener('click', function (e) {
        e.preventDefault();
        formContainer.classList.remove('is-collapsed');
        formContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    }

    if (btnCancel && formContainer) {
      btnCancel.addEventListener('click', function (e) {
        e.preventDefault();
        formContainer.classList.add('is-collapsed');
        if (fileCard) {
          fileCard.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      });
    }

    if (fileInput && fileNameDisplay) {
      fileInput.addEventListener('change', function () {
        if (fileInput.files && fileInput.files.length > 0) {
          fileNameDisplay.textContent = 'Đã chọn tệp: ' + fileInput.files[0].name;
          fileNameDisplay.style.color = '#0284c7';
          fileNameDisplay.style.fontWeight = '600';
        } else {
          fileNameDisplay.textContent = 'Chưa có tệp nào được chọn';
          fileNameDisplay.style.color = '';
          fileNameDisplay.style.fontWeight = '';
        }
      });
    }

    if (dropzone && fileInput) {
      ['dragenter', 'dragover'].forEach(function (eventName) {
        dropzone.addEventListener(eventName, function (e) {
          e.preventDefault();
          e.stopPropagation();
          dropzone.classList.add('dragover');
        });
      });

      ['dragleave', 'drop'].forEach(function (eventName) {
        dropzone.addEventListener(eventName, function (e) {
          e.preventDefault();
          e.stopPropagation();
          dropzone.classList.remove('dragover');
        });
      });

      dropzone.addEventListener('drop', function (e) {
        if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length > 0) {
          fileInput.files = e.dataTransfer.files;
          const event = new Event('change');
          fileInput.dispatchEvent(event);
        }
      });
    }
  }

  // ===== Module Confirm Modal =====
  function initConfirmModal() {
    const modal = document.getElementById('confirmModal');
    const msgEl = document.getElementById('confirmModalMessage');
    const titleEl = document.getElementById('confirmModalTitle');
    const acceptBtn = document.getElementById('confirmModalAccept');
    let pendingForm = null;

    function openConfirmModal(form) {
      pendingForm = form;
      const msg = form.getAttribute('data-confirm') || 'Bạn có chắc chắn muốn thực hiện thao tác này?';
      const title = form.getAttribute('data-modal-title') || 'Thông báo';
      if (msgEl) msgEl.textContent = msg;
      if (titleEl) titleEl.textContent = title;
      if (modal) {
        modal.style.display = 'flex';
        modal.classList.add('open');
        modal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('modal-open');
      }
    }

    function closeConfirmModal() {
      if (modal) {
        modal.classList.remove('open');
        modal.setAttribute('aria-hidden', 'true');
        modal.style.display = 'none';
        document.body.classList.remove('modal-open');
      }
      pendingForm = null;
    }

    // Lắng nghe submit trên mọi form có data-confirm
    document.addEventListener('submit', function (e) {
      const form = e.target.closest('form[data-confirm]');
      if (!form) return;

      // Fallback nếu modal hoặc nút xác nhận không tồn tại
      if (!modal || !acceptBtn) {
        const msg = form.getAttribute('data-confirm') || 'Bạn có chắc chắn muốn thực hiện thao tác này?';
        if (!window.confirm(msg)) {
          e.preventDefault();
        }
        return;
      }

      e.preventDefault();
      openConfirmModal(form);
    });

    if (modal) {
      modal.querySelectorAll('[data-modal-cancel]').forEach(function(btn) {
        btn.addEventListener('click', function (e) {
          e.preventDefault();
          closeConfirmModal();
        });
      });

      // Click vào backdrop bên ngoài để đóng modal
      modal.addEventListener('click', function (e) {
        if (e.target.classList.contains('portal-modal-backdrop') || e.target === modal) {
          closeConfirmModal();
        }
      });
    }

    document.addEventListener('keydown', function (e) {
      if (!modal || !modal.classList.contains('open')) return;
      if (e.key === 'Escape') { e.preventDefault(); closeConfirmModal(); }
    });

    if (acceptBtn) {
      acceptBtn.addEventListener('click', function (e) {
        e.preventDefault();
        if (pendingForm) {
          const formToSubmit = pendingForm;
          pendingForm = null;
          closeConfirmModal();

          const savedConfirm = formToSubmit.getAttribute('data-confirm');
          formToSubmit.removeAttribute('data-confirm');

          // Submit form
          formToSubmit.submit();

          setTimeout(function() {
            if (savedConfirm) formToSubmit.setAttribute('data-confirm', savedConfirm);
          }, 500);
        }
      });
    }
  }

  initNopBaoCaoModule();
  initConfirmModal();
  initHeaderNotices();
});