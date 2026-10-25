const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.resolve(__dirname, '../..', '..');
const source = name => fs.readFileSync(path.join(root, 'src/main/resources/static/js', name), 'utf8');
class Element {
  constructor(value = '') {
    this.value = value; this.listeners = {}; this.style = {}; this.children = []; this.attrs = {}; this.disabled = false;
    const values = new Set();
    this.classList = {add: v => values.add(v), remove: v => values.delete(v), contains: v => values.has(v)};
  }
  addEventListener(event, fn) { (this.listeners[event] ??= []).push(fn); }
  emit(event, e = {}) { e.preventDefault ??= function () { this.defaultPrevented = true; }; return (this.listeners[event] ?? []).map(fn => fn(e)); }
  setAttribute(key, value) { this.attrs[key] = value; }
  getAttribute(key) { return this.attrs[key] ?? null; }
  appendChild(node) { this.children.push(node); node.parentNode = this; return node; }
  removeChild(node) { this.children = this.children.filter(n => n !== node); node.parentNode = null; }
  querySelector(selector) { return (this.parts ??= {})[selector] ??= new Element(); }
  querySelectorAll() { return []; }
  contains(node) { return node === this || this.children.some(child => child.contains(node)); }
  focus() { this.focused = true; }
}
function exportFixture(response, format = 'xlsx', prefix = '') {
  const button = new Element(), item = new Element(), dropdown = new Element(), wrap = new Element(), container = new Element();
  const downloads = [], requests = [], blobs = []; let reads = 0, ready;
  const href = prefix + '/morong/xuat/detai.' + format + '?keyword=Ti%E1%BA%BFng%20Vi%E1%BB%87t';
  Object.assign(item.attrs, {href, 'data-export-format': format, 'data-export-label': format});
  wrap.parts = {'.morong-export-btn': button, '.morong-export-dropdown': dropdown}; dropdown.querySelectorAll = () => [item];
  const document = {body: new Element(), querySelector: () => container, querySelectorAll: () => [wrap],
    addEventListener: (event, fn) => { if (event === 'DOMContentLoaded') ready = fn; },
    createTextNode: text => Object.assign(new Element(), {textContent: text}),
    createElement: tag => { const e = new Element(); e.click = () => downloads.push({filename: e.download, href: e.href}); return e; }};
  const bytes = format === 'pdf' ? [37,80,68,70,45,49,46,55] : [80,75,3,4,0,0,0,0];
  const headers = {'content-type': format === 'pdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'content-disposition': 'attachment; filename="de_tai_thu.' + format + '"'};
  Object.assign(headers, response.headers);
  const reply = {status: 200, redirected: false, url: 'https://portal.example' + href, ...response,
    headers: {get: key => headers[key] ?? null}, arrayBuffer: async () => { reads++; return Uint8Array.from(response.bytes ?? bytes).buffer; }};
  vm.runInNewContext(source('morong-export.js'), {document,
    window: {location: {origin: 'https://portal.example', href: 'https://portal.example' + prefix + '/detai'},
      URL: {createObjectURL: () => 'blob:test', revokeObjectURL() {}}},
    URL, Uint8Array, Blob: class {constructor(parts, options) {blobs.push({parts, options});}},
    fetch: async (...args) => {requests.push(args);return reply;}, setTimeout: () => 1, clearTimeout() {}});
  ready();
  return {button, requests, downloads, blobs, container, reads: () => reads,
    click: () => Promise.all(item.emit('click', {preventDefault() {}})),
    messages: () => container.children.map(t => t.querySelector('.morong-export-toast-desc').textContent).join('\n')};
}
for (const [name, response] of [
  ['200 HTML', {headers: {'content-type': 'text/html'}}],
  ['200 JSON', {headers: {'content-type': 'application/json'}}],
  ['redirect login', {redirected: true, url: 'https://portal.example/login'}],
  ['final login without redirect flag', {url: 'https://portal.example/login'}],
  ['redirect other', {redirected: true}], ['cross-origin final URL', {url: 'https://other.invalid/file.xlsx'}],
  ['401', {status: 401}], ['403', {status: 403}], ['422 JSON', {status: 422, headers: {'content-type': 'application/json'}}],
  ['503', {status: 503}], ['204', {status: 204}], ['partial 206', {status: 206}],
  ['missing disposition', {headers: {'content-disposition': null}}],
  ['wrong extension', {headers: {'content-disposition': 'attachment; filename="error.html"'}}],
  ['inline disposition', {headers: {'content-disposition': 'inline; filename="file.xlsx"'}}],
  ['path filename', {headers: {'content-disposition': 'attachment; filename="../file.xlsx"'}}],
  ['HTML body with fake valid headers', {bytes: Array.from(Buffer.from('<html>error</html>'))}],
  ['empty body', {bytes: []}], ['wrong length', {headers: {'content-length': '12345'}}]
]) test('rejects ' + name + ' without Blob/download/success', async () => {
  const f = exportFixture(response); await f.click();
  assert.equal(f.downloads.length, 0);assert.equal(f.blobs.length, 0);assert.equal(f.button.disabled, false);
  assert(!f.container.children.some(t => t.className.includes('toast-success')));
  if (!response.bytes) assert.equal(f.reads(), name === 'wrong length' ? 1 : 0);
});
test('expired session links to same application login, including context path', async () => {
  const f = exportFixture({redirected: true, url: 'https://portal.example/truong/login'}, 'xlsx', '/truong');await f.click();
  assert.match(f.messages(), /đăng nhập lại/);
  assert(f.container.children.flatMap(t => t.querySelector('.morong-export-toast-desc').children)
    .some(e => e.href === 'https://portal.example/truong/login'));
});
for (const format of ['xlsx','pdf']) test('valid '+format+' downloads exactly once with server filename', async () => {
  const f = exportFixture({}, format);await f.click();
  assert.equal(f.downloads.length, 1);assert.equal(f.downloads[0].filename, 'de_tai_thu.' + format);assert.equal(f.blobs.length, 1);
  assert(f.container.children.some(t => t.className.includes('toast-success')));
  assert.equal(f.requests[0][1].credentials, 'same-origin');
});
test('UTF-8 filename is retained as text and repeated clicks share one in-flight request', async () => {
  const f = exportFixture({headers: {'content-disposition': "attachment; filename*=UTF-8''%C4%90%E1%BB%81%20t%C3%A0i.xlsx"}});
  await Promise.all([f.click(), f.click()]);assert.equal(f.requests.length, 1);assert.equal(f.downloads[0].filename, 'Đề tài.xlsx');
});
test('backend diagnostics are never rendered as HTML or echoed in a toast', async () => {
  const f = exportFixture({status: 503, json: async () => ({message: '<img onerror=alert(1)> SQL_PASSWORD stack trace'})});
  await f.click();assert(!f.messages().includes('SQL_PASSWORD'));assert(!f.messages().includes('onerror'));
});

function searchFixture({nativeValid = true, disclosure = false} = {}) {
  const fields = {pageNumberInput: new Element('3'), filterForm: new Element(), size: new Element('25')};
  if(disclosure) {
    fields.detaiAdvancedFilters = new Element();fields.detaiAdvancedFilters.open = false;
    fields.detaiAdvancedSummary = new Element();fields.sort = new Element('id,desc');
    fields.keyword = new Element('Tiếng Việt');fields.dotdangkyid = new Element('8');
    fields.detaiAdvancedFilters.appendChild(fields.sort);fields.detaiAdvancedFilters.appendChild(fields.dotdangkyid);
    fields.detaiAdvancedFilters.appendChild(fields.size);
  }
  let ready; const window = new Element(), requests = [], document = {
    getElementById: id => fields[id] ?? null,
    addEventListener: (event, fn) => {if(event === 'DOMContentLoaded') ready = fn;}
  };
  vm.runInNewContext(source('morong-search.js'), {document,window});ready();
  fields.filterForm.requestSubmit = () => {
    if(!nativeValid)return;
    const event={defaultPrevented:false,preventDefault(){this.defaultPrevented=true;}};
    fields.filterForm.emit('submit',event);
    if(!event.defaultPrevented)requests.push({page:fields.pageNumberInput.value,size:fields.size.value,...(disclosure?{keyword:fields.keyword.value,dotdangkyid:fields.dotdangkyid.value,sort:fields.sort.value}:{})});
  };
  return {fields,window,requests};
}
test('changing a topic filter resets the page and submission is not duplicated', () => {
  const f=searchFixture();f.fields.filterForm.emit('input');assert.equal(f.fields.pageNumberInput.value,'0');
  f.fields.filterForm.requestSubmit();f.fields.filterForm.requestSubmit();assert.deepEqual(f.requests,[{page:'0',size:'25'}]);
});
test('browser native validation blocks submission and bfcache restores the form', () => {
  const invalid=searchFixture({nativeValid:false});invalid.fields.filterForm.requestSubmit();assert.equal(invalid.requests.length,0);
  const f=searchFixture();f.fields.filterForm.emit('change');f.fields.filterForm.requestSubmit();
  f.window.emit('pageshow');f.fields.filterForm.requestSubmit();assert.equal(f.requests.length,2);
});

test('scripts leave pages without Module 17 controls alone', () => {
  const document={addEventListener:(name,fn)=>fn(),getElementById:()=>null,querySelectorAll:()=>[],querySelector:()=>null};
  vm.runInNewContext(source('morong-search.js'),{document});vm.runInNewContext(source('morong-export.js'),{document});
});

test('native disclosure toggle synchronizes ARIA repeatedly without requests, page reset or losing filter values', () => {
  const f=searchFixture({disclosure:true});const details=f.fields.detaiAdvancedFilters;
  for(let i=0;i<20;i++) {
    details.open=!details.open;details.emit('toggle');
    assert.equal(f.fields.detaiAdvancedSummary.getAttribute('aria-expanded'),String(details.open));
  }
  assert.equal(f.requests.length,0);assert.equal(f.fields.pageNumberInput.value,'3');
  assert.equal(f.fields.dotdangkyid.value,'8');assert.equal(f.fields.sort.value,'id,desc');
  f.window.resetPage();f.fields.filterForm.requestSubmit();
  assert.deepEqual(f.requests,[{page:'0',size:'25',keyword:'Tiếng Việt',dotdangkyid:'8',sort:'id,desc'}]);
});
test('invalid advanced control is revealed for native validation without submitting', () => {
  const f=searchFixture({disclosure:true});
  f.fields.filterForm.emit('invalid',{target:f.fields.keyword});assert.equal(f.fields.detaiAdvancedFilters.open,false);
  f.fields.filterForm.emit('invalid',{target:f.fields.sort});assert.equal(f.fields.detaiAdvancedFilters.open,true);
  assert.equal(f.fields.detaiAdvancedSummary.getAttribute('aria-expanded'),'true');assert.equal(f.requests.length,0);
});
test('Back/Forward pageshow synchronizes restored disclosure and retains keyword, filters, sort, size and page', () => {
  const f=searchFixture({disclosure:true});f.fields.detaiAdvancedFilters.open=true;f.fields.sort.value='tendetai,asc';
  f.fields.size.value='37';f.window.emit('pageshow');
  assert.equal(f.fields.detaiAdvancedSummary.getAttribute('aria-expanded'),'true');
  f.fields.filterForm.requestSubmit();f.window.emit('pageshow');f.fields.detaiAdvancedFilters.open=false;f.window.emit('pageshow');
  f.fields.filterForm.requestSubmit();assert.equal(f.requests.length,2);
  assert.deepEqual(f.requests[0],f.requests[1]);assert.equal(f.requests[1].page,'3');assert.equal(f.requests[1].size,'37');
  assert.equal(f.fields.detaiAdvancedSummary.getAttribute('aria-expanded'),'false');
});
