document.addEventListener("DOMContentLoaded", function () {
  const wrap = document.getElementById("headerNoticesWrap");
  const btn = document.getElementById("headerNoticeBtn");
  const badge = document.getElementById("headerNoticeBadge");
  if (!wrap || !btn || !badge) return;
  const feed = wrap.dataset.noticeFeedUrl;
  const body = wrap.querySelector(".notice-dropdown-body");
  const loadStatus = document.getElementById("headerNoticeLoadStatus");
  let version = 0;
  const icon = wrap.querySelector(".notice-item-icon svg") || wrap.querySelector(".notice-title-icon");
  function showStatus(element, message) {
    if (!element) return;
    element.textContent = message; element.hidden = !message;
  }
  function updateCount(count) {
    if (!Number.isSafeInteger(count) || count < 0) throw new Error("Invalid unread count");
    badge.textContent = count > 99 ? "99+" : String(count);
    badge.hidden = count === 0;
    badge.setAttribute("aria-label", count + " thông báo chưa đọc");
    btn.setAttribute("aria-label", count ? "Thông báo, " + count + " chưa đọc" : "Thông báo");
  }
  function render(data) {
    if (!Array.isArray(data.items) || data.items.length > 5) throw new Error("Invalid notice list");
    updateCount(data.unreadCount);
    const focusedId = body && body.contains(document.activeElement) ? document.activeElement.dataset.noticeId : null;
    const list = document.createElement("div"); list.className = "notice-list";
    data.items.forEach(item => {
      if (!Number.isSafeInteger(item.id) || item.id <= 0) throw new Error("Invalid notice ID");
      const link = document.createElement("a");
      link.className = "notice-item" + (item.unread ? " notice-item-unread" : "");
      link.dataset.noticeId = String(item.id);
      link.href = wrap.dataset.noticeDetailBase + encodeURIComponent(item.id);
      link.setAttribute("aria-label", item.title + " · " + (item.unread ? "Chưa đọc" : "Đã xem hoặc thông báo lịch sử"));
      if (icon) {
        const iconWrap = document.createElement("div"); iconWrap.className = "notice-item-icon";
        iconWrap.setAttribute("aria-hidden", "true");
        iconWrap.appendChild(icon.cloneNode(true)); link.appendChild(iconWrap);
      }
      const content = document.createElement("div"); content.className = "notice-item-content";
      const title = document.createElement("div"); title.className = "notice-item-title"; title.textContent = item.title;
      const meta = document.createElement("div"); meta.className = "notice-item-meta";
      const author = document.createElement("span"); author.className = "notice-item-author"; author.textContent = item.author;
      const date = document.createElement("span"); date.className = "notice-item-date";
      const match = typeof item.postedAt === "string" && item.postedAt.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}:\d{2})/);
      date.textContent = match ? match[3] + "/" + match[2] + "/" + match[1] + " " + match[4] : "";
      meta.append(author, date); content.append(title, meta); link.appendChild(content); list.appendChild(link);
    });
    if (body) {
      if (data.items.length) body.replaceChildren(list);
      else {
        const empty = document.createElement("div"); empty.className = "notice-empty";
        empty.textContent = "Chưa có thông báo nào"; body.replaceChildren(empty);
      }
      if (focusedId) {
        const previous = Array.from(body.querySelectorAll("a[data-notice-id]")).find(a => a.dataset.noticeId === focusedId);
        if (previous) previous.focus(); else btn.focus();
      }
    }
    showStatus(loadStatus, "");
  }
  async function refresh() {
    if (!feed) return;
    const request = ++version;
    try {
      const response = await fetch(feed, { credentials: "same-origin", cache: "no-store", headers: { Accept: "application/json" } });
      if (!response.ok || !response.headers.get("content-type")?.includes("application/json")) throw new Error("Notice feed unavailable");
      const data = await response.json(); if (request === version) render(data);
    } catch (error) {
      if (request === version) showStatus(loadStatus, "Chưa tải được trạng thái mới. Hãy tải lại trang; số đang hiển thị có thể đã cũ.");
    }
  }
  // app.js continues to own open/close/Escape/arrow navigation; opening only performs a GET.
  btn.addEventListener("click", function () { if (btn.getAttribute("aria-expanded") === "true") refresh(); });
  window.addEventListener("pageshow", function (event) { if (event.persisted) refresh(); });
  const form = document.getElementById("noticeMarkReadForm");
  async function markLoadedDetail() {
    if (!feed || !form) return;
    const status = document.getElementById("noticeReadStatus");
    ++version; // Discard any earlier feed response; it predates the POST.
    try {
      const response = await fetch(form.action, { method: "POST", credentials: "same-origin", cache: "no-store",
        headers: { Accept: "application/json" }, body: new URLSearchParams(new FormData(form)) });
      if (!response.ok || !response.headers.get("content-type")?.includes("application/json")) throw new Error("Read status unavailable");
      const state = await response.json(); ++version; updateCount(state.unreadCount);
      showStatus(status, ""); await refresh();
    } catch (error) {
      showStatus(status, "Chưa ghi nhận đã đọc. Bạn có thể tải lại trang để thử lại; nội dung vẫn xem được.");
    }
  }
  // No GET side effects, no timer/polling. Wait for the successfully rendered detail page to finish loading.
  if (document.readyState === "complete") markLoadedDetail();
  else window.addEventListener("load", markLoadedDetail, { once: true });
});
