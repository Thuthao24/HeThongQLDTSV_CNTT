const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/morong-thongke.js'),'utf8');
class Element {
  constructor() { this.listeners={};this.attrs={};this.classList={remove(){}}; }
  addEventListener(type,fn,capture=false) { (this.listeners[type]??=[]).push({fn,capture}); }
  emit(type) { const event={preventDefault(){this.defaultPrevented=true;},stopImmediatePropagation(){this.stopped=true;}};
    for(const {fn} of this.listeners[type]??[]) {fn(event);if(event.stopped)break;} return event; }
  setAttribute(key,value) {this.attrs[key]=value;}
}
function fixture(applied='',restored=applied) {
  const page=new Element(),form=new Element(),select=new Element(),wrap=new Element(),button=new Element(),window=new Element();
  const links=[new Element(),new Element()];let ready;const gets=[];
  page.dataset={selectedRound:applied};page.querySelector=()=>wrap;select.value=restored;wrap.querySelectorAll=()=>links;
  const document={querySelector:()=>page,getElementById:id=>({filterForm:form,dotdangkyid:select,exportDropdownBtn:button})[id],addEventListener:(_,fn)=>ready=fn};
  form.requestSubmit=()=>{const event=form.emit('submit');if(!event.defaultPrevented)gets.push({method:'GET',round:select.value});};
  vm.runInNewContext(source,{document,window});ready();
  return {page,form,select,wrap,button,links,window,gets,choose(value){select.value=value;select.emit('change');}};
}
test('selection submits one ordinary GET immediately, without AJAX',()=>{
  const f=fixture();f.choose('2');assert.deepEqual(f.gets,[{method:'GET',round:'2'}]);assert.equal(f.button.disabled,true);
  assert.equal(f.form.attrs['aria-busy'],'true');assert(f.links.every(l=>l.attrs['aria-disabled']==='true'));
});
for(const type of ['click','auxclick','contextmenu']) {
  test('blocks stale '+type+' export while new GET is in flight',()=>{
    const f=fixture();f.choose('2');const e=f.wrap.emit(type);assert(e.defaultPrevented);assert(e.stopped);
    assert.equal(f.wrap.listeners[type][0].capture,true);
  });
  test('blocks '+type+' even if select changed before its change event fires',()=>{
    const f=fixture();f.select.value='2';assert(f.wrap.emit(type).stopped);assert.equal(f.gets.length,0);
  });
}
test('same scope and spurious repeated change cannot submit duplicate GETs',()=>{
  const f=fixture('2');f.choose('2');assert.equal(f.gets.length,0);f.choose('3');f.choose('3');assert.equal(f.gets.length,1);
});
test('after GET the selected round is applied and export is available',()=>{
  const f=fixture('2');assert.equal(f.select.value,'2');assert.equal(f.button.disabled,false);assert(!f.wrap.emit('click').stopped);
});
test('going from selected round to all submits empty scope, never the old ID',()=>{
  const f=fixture('2');f.choose('');assert.deepEqual(f.gets,[{method:'GET',round:''}]);
});
for(const [label,applied,restored] of [['Back','','2'],['Forward','2',''],['reload','3','1']])test(label+' reconciles restored form with server scope',()=>{
  const f=fixture(applied,restored);assert.equal(f.select.value,applied);assert.equal(f.gets.length,0);
  f.choose('5');assert(f.button.disabled);f.window.emit('pageshow');assert.equal(f.select.value,applied);assert.equal(f.button.disabled,false);
  assert(!f.wrap.emit('click').stopped);
});
test('multiple history entries retain independent server scopes and reset pending state',()=>{
  const all=fixture(),first=fixture('1'),second=fixture('2');all.choose('1');first.choose('2');
  for(const f of [all,first,second,first,all]) {f.window.emit('pageshow');assert.equal(f.select.value,f.page.dataset.selectedRound);assert(!f.button.disabled);}
});

test('all-round HTML may omit empty data attribute: selection and exports stay enabled',()=>{
  const f=fixture(null,'');assert.equal(f.select.value,'');assert.equal(f.button.disabled,false);assert(!f.wrap.emit('click').stopped);f.choose('2');assert.equal(f.gets[0].round,'2');
});
