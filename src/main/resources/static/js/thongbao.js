document.addEventListener("DOMContentLoaded", function () {
  const form = document.querySelector("[data-notice-form]");
  if (!form) return;
  const all = form.querySelector("[data-audience-all]");
  const roles = Array.from(form.querySelectorAll("[data-audience-role]"));
  function syncAudience() {
    if (!all || roles.length === 0) return;
    const count = roles.filter(role => role.checked).length;
    all.checked = count === roles.length;
    all.indeterminate = count > 0 && count < roles.length;
    roles[0].setCustomValidity(count ? "" : "Vui lòng chọn ít nhất một đối tượng nhận thông báo.");
  }
  if (all) all.addEventListener("change", function () {
    roles.forEach(role => { role.checked = all.checked; }); syncAudience();
  });
  roles.forEach(role => role.addEventListener("change", syncAudience));
  syncAudience();
  let submitting = false;
  let awaitingConfirmation = false;
  let confirmed = false;
  let submitter = null;
  const dialog = document.getElementById("noticeAudienceDialog");
  const cancelButton = document.getElementById("noticeAudienceCancel");
  const confirmButton = document.getElementById("noticeAudienceConfirm");
  const originalValue = form.querySelector('input[name="originalRecipientRoles"]')?.value;
  const original = new Set(originalValue ? originalValue.split(",").filter(Boolean) : roles.filter(role => role.checked).map(role => role.value));
  const warning = "Bạn đang thay đổi đối tượng nhận thông báo. Những đối tượng bị loại sẽ không còn xem được thông báo này. Bạn có chắc chắn muốn lưu thay đổi?";
  function removedAudience() {
    const selected = new Set(roles.filter(role => role.checked).map(role => role.value));
    return Array.from(original).some(role => !selected.has(role));
  }
  function cancelChange() {
    awaitingConfirmation = false; confirmed = false;
    if (dialog?.open) dialog.close();
    if (submitter) submitter.focus();
  }
  function confirmChange() {
    if (!awaitingConfirmation || submitting) return;
    awaitingConfirmation = false; confirmed = true;
    if (confirmButton) confirmButton.disabled = true;
    if (dialog?.open) dialog.close();
    // requestSubmit preserves native validation, CSRF fields, revision and the existing submit guard.
    form.requestSubmit(submitter || undefined);
    confirmed = false; // Validation may have blocked dispatch; never retain approval for a later edit.
  }
  if (cancelButton) cancelButton.addEventListener("click", cancelChange);
  if (confirmButton) confirmButton.addEventListener("click", confirmChange);
  if (dialog) {
    dialog.addEventListener("cancel", function (event) { event.preventDefault(); cancelChange(); });
    dialog.addEventListener("close", function () { if (awaitingConfirmation && !dialog.open) cancelChange(); });
  }
  form.addEventListener("submit", function (event) {
    if (submitting) { event.preventDefault(); return; }
    syncAudience();
    if (!form.checkValidity()) { event.preventDefault(); form.reportValidity(); return; }
    if (form.dataset.noticeMode === "EDIT" && removedAudience() && !confirmed) {
      event.preventDefault();
      if (awaitingConfirmation) return;
      submitter = event.submitter || form.querySelector('button[type="submit"]');
      awaitingConfirmation = true;
      if (confirmButton) confirmButton.disabled = false;
      if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal(); // Native dialog traps focus and makes the background inert.
        cancelButton.focus();
      } else {
        // Keep confirmation on browsers without dialog support; modern Windows browsers use the styled dialog.
        if (window.confirm(warning)) confirmChange(); else cancelChange();
      }
      return;
    }
    confirmed = false;
    submitting = true;
    form.querySelectorAll('button[type="submit"]').forEach(button => { button.disabled = true; });
  });
  // Back/forward cache must not leave an otherwise usable form disabled.
  window.addEventListener("pageshow", function () {
    syncAudience();
    cancelChange();
    submitting = false;
    form.querySelectorAll('button[type="submit"]').forEach(button => { button.disabled = false; });
  });
});
