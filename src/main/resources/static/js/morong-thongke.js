/**
 * MODULE 17: GIAI ĐOẠN 2 - DASHBOARD THỐNG KÊ
 * Namespace: morongThongke
 */
document.addEventListener("DOMContentLoaded", function () {
  // Export Dropdown handling
  const exportWrap = document.querySelector(".morong-thongke-export-wrap");
  const exportBtn = document.getElementById("exportDropdownBtn");

  if (exportWrap && exportBtn) {
    exportBtn.addEventListener("click", function (e) {
      e.stopPropagation();
      const isOpen = exportWrap.classList.contains("is-open");
      exportWrap.classList.toggle("is-open", !isOpen);
      exportBtn.setAttribute("aria-expanded", String(!isOpen));
    });

    document.addEventListener("click", function (e) {
      if (!exportWrap.contains(e.target)) {
        exportWrap.classList.remove("is-open");
        exportBtn.setAttribute("aria-expanded", "false");
      }
    });

    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && exportWrap.classList.contains("is-open")) {
        exportWrap.classList.remove("is-open");
        exportBtn.setAttribute("aria-expanded", "false");
        exportBtn.focus();
      }
    });
  }
});
