/* Statistics uses normal GET navigation; export validation remains in morong-export.js. */
document.addEventListener("DOMContentLoaded", function () {
  const page = document.querySelector("[data-statistics]");
  const form = document.getElementById("filterForm");
  const select = document.getElementById("dotdangkyid");
  if (!page || !form || !select) return;
  const applied = page.dataset.selectedRound || "";
  const wrap = page.querySelector(".morong-export-wrap");
  const button = document.getElementById("exportDropdownBtn");
  let pending = false;

  function setPending(value) {
    pending = value;
    form.setAttribute("aria-busy", String(value));
    if (button) { button.disabled = value; button.setAttribute("aria-expanded", "false"); }
    if (wrap) {
      wrap.classList.remove("is-open");
      wrap.querySelectorAll("a[data-export-format]").forEach(link => link.setAttribute("aria-disabled", String(value)));
    }
  }
  // Capture before the shared downloader: never export the previously rendered round
  // while a new GET is in flight, including keyboard and middle-button activation.
  if (wrap) ["click", "auxclick", "contextmenu"].forEach(type => {
    wrap.addEventListener(type, function (event) {
      if (pending || select.value !== applied) {
        event.preventDefault(); event.stopImmediatePropagation();
      }
    }, true);
  });
  form.addEventListener("submit", function () { setPending(true); });
  select.addEventListener("change", function () {
    if (!pending && select.value !== applied) form.requestSubmit();
  });
  // Browsers may restore form values independently of cached HTML on Back/Forward.
  // The server-rendered scope belongs to this history entry and its export links.
  function restoreScope() { select.value = applied; setPending(false); }
  restoreScope();
  window.addEventListener("pageshow", restoreScope);
});
