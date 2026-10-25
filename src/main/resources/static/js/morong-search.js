document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("filterForm");
  const page = document.getElementById("pageNumberInput");
  if (!form || !page) return;
  let submitting = false;
  const advanced = document.getElementById("detaiAdvancedFilters");
  const summary = document.getElementById("detaiAdvancedSummary");
  function syncExpanded() {
    if (advanced && summary) summary.setAttribute("aria-expanded", String(advanced.open));
  }
  if (advanced && summary) {
    // Native details handles click, Enter and Space without submitting or disabling any inputs.
    advanced.addEventListener("toggle", syncExpanded);
    // Reveal an invalid control before native form validation tries to focus it.
    form.addEventListener("invalid", function (event) {
      if (advanced.contains(event.target)) { advanced.open = true; syncExpanded(); }
    }, true);
    syncExpanded();
  }

  window.resetPage = function () { page.value = "0"; };
  form.addEventListener("input", window.resetPage);
  form.addEventListener("change", window.resetPage);
  form.addEventListener("submit", function (event) {
    if (submitting) { event.preventDefault(); return; }
    if (event.defaultPrevented) return;
    submitting = true;
  });
  window.addEventListener("pageshow", function () { submitting = false; syncExpanded(); });
});
