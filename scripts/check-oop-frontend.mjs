import assert from 'node:assert/strict';
import {UiKit} from '../src/main/resources/static/js/core/UiKit.js';
import {BasePage} from '../src/main/resources/static/js/core/BasePage.js';
import {StudentPage} from '../src/main/resources/static/js/core/StudentPage.js';
import {AdminPage} from '../src/main/resources/static/js/core/AdminPage.js';
import {ActionHandler} from '../src/main/resources/static/js/components/ActionHandler.js';
import {Hold} from '../src/main/resources/static/js/models/Hold.js';
import {WindowBanner} from '../src/main/resources/static/js/components/WindowBanner.js';
import {AdvisingPage} from '../src/main/resources/static/js/pages/student/AdvisingPage.js';
import {StudentDashboardPage} from '../src/main/resources/static/js/pages/student/StudentDashboardPage.js';

const kit=new UiKit(),view=kit.bindings();
assert.match(view.table([{name:'<student>'}],[['Student','name']]),/&lt;student&gt;/);
assert.match(view.input('Capacity','capacity','number',0),/value="0"/);
assert.match(view.select('Department','department',[['CSE','Computing']],'CSE'),/selected/);
const slotMarkup=[0,1].map(index=>view.select('Day','day',[['SAT','Saturday']],'SAT','slot-'+index+'-day')+view.input('Start','start','time','09:00',true,'slot-'+index+'-start')).join('');
const slotIds=[...slotMarkup.matchAll(/\bid="([^"]+)"/g)].map(match=>match[1]);
assert.equal(new Set(slotIds).size,slotIds.length,'Repeated schedule fields must have unique IDs.');
for(const match of slotMarkup.matchAll(/\bfor="([^"]+)"/g))assert.ok(slotIds.includes(match[1]),'A field label must reference its own control.');
assert.doesNotMatch(view.routine([],'No teaching schedule yet.','Assigned sections and their class times will appear here.'),/Confirm courses in Advising/);
const hold=new Hold('2026-11-06T10:10:00+06:00');
assert.equal(hold.display(new Date('2026-11-06T10:00:00+06:00').getTime()),'00:10:00');
assert.equal(hold.isExpired(new Date('2026-11-06T10:10:00+06:00').getTime()),true);
assert.equal(new StudentPage('advising').requiredRole,'STUDENT');
assert.equal(new AdminPage('offerings').requiredRole,'ADMIN');

// Exercise the open-window branch used by student Advising and Dashboard.
const banner=new WindowBanner(kit),now=Date.now();
const openWindow={status:'OPEN',start_at:new Date(now-60000).toISOString(),end_at:new Date(now+3600000).toISOString()};
const openBanner=banner.render([openWindow]);
assert.match(openBanner,/Advising is open/);
assert.match(openBanner,/Continue to Advising/);
assert.ok(openBanner.includes('data-until="'+openWindow.end_at+'"'));
assert.match(banner.render([]),/Advising is not open/);
assert.match(banner.render([{...openWindow,status:'CLOSED'}]),/Advising is not open/);
assert.match(banner.render([{...openWindow,end_at:new Date(now-1000).toISOString()}]),/Advising is not open/);
assert.match(banner.render([{...openWindow,status:'SCHEDULED',start_at:new Date(now+60000).toISOString()}]),/Next window:/);
const nearer={...openWindow,status:'SCHEDULED',start_at:new Date(now+60000).toISOString()};
const later={...nearer,start_at:new Date(now+7200000).toISOString()};
assert.equal(banner.render([later,nearer]),banner.render([nearer]),'Next session must remain the nearest scheduled window regardless of display order.');

globalThis.document={querySelectorAll:()=>[]};
for(const PageClass of [AdvisingPage,StudentDashboardPage]){
 const studentPage=new PageClass(),nodes=new Map(),content={innerHTML:''};
 const user={full_name:'Student Test',login_id:'S1',role:'STUDENT'};
 const course={id:1,code:'CSE101',name:'Computer Fundamentals',credit:3,faculty:'Kabir',schedule:'SAT 09:00-10:30',room:'Room 1',available:39,capacity:40};
 const profile={cgpa:null,status:'NEW',total_credits_completed:0,department:'Computing',semester:'Fall'};
 Object.assign(studentPage.context,{content,user,ref:{semesters:[{is_current:true,name:'Fall'}]}});
 const original=studentPage.ui.bindings();
 studentPage.ui.bindings=()=>({...original,
  $(selector){if(!nodes.has(selector))nodes.set(selector,{innerHTML:'',textContent:'',style:{}});return nodes.get(selector);},
  actions(){},
  async api(path){
   if(path==='/advising/state')return {windows:[openWindow],window:openWindow,credit:0,band:{max_credit:15,min_credit:3,status_label:'NEW'},holds:[],confirmed:[]};
   if(path==='/advising/courses')return [course];
   if(path==='/notices')return [];
   if(path==='/dashboard/me')return {windows:[openWindow],profile,routine:[],selectedCredit:0};
   throw new Error('Unexpected student page request: '+path);
  }
 });
 studentPage.every=()=>{};
 await studentPage.render();
 if(PageClass===AdvisingPage){
  assert.match(nodes.get('#window-banner').innerHTML,/Advising is open/);
  assert.match(nodes.get('#open-courses').innerHTML,/CSE101/);
  assert.match(nodes.get('#open-courses').innerHTML,/data-action="hold-select"/);
  assert.match(nodes.get('#holds').innerHTML,/No courses on hold/);
 }else assert.match(content.innerHTML,/Advising is open/);
 studentPage.destroy();
}

globalThis.window=new EventTarget();
const stages=[];
class LifecyclePage extends BasePage {
    async guard(){stages.push('guard');return true;}
    mountLayout(){stages.push('layout');}
    async load(){stages.push('load');}
    async render(){stages.push('render');}
    bind(){stages.push('bind');}
}
const page=new LifecyclePage('test');await page.init();
assert.deepEqual(stages,['guard','layout','load','render','bind']);page.destroy();

// A single event dispatcher lets shell actions and page actions coexist.
const clicked={disabled:false,dataset:{action:'save',id:'1'}};
class TestDocument extends EventTarget {closest(){return clicked;}}
globalThis.document=new TestDocument();
const calls=[],actions=new ActionHandler(kit);
actions.render(async action=>calls.push('page:'+action));
actions.render(async action=>calls.push('shell:'+action));
document.dispatchEvent(new Event('click'));
await new Promise(resolve=>setTimeout(resolve,0));
assert.deepEqual(calls,['page:save','shell:save']);assert.equal(clicked.disabled,false);
kit.destroy();document.dispatchEvent(new Event('click'));
await new Promise(resolve=>setTimeout(resolve,0));assert.equal(calls.length,2);
const printPage=new StudentPage('routine');
let shellAction,prints=0;
window.print=()=>prints++;
const printBindings=printPage.ui.bindings();
printPage.ui.bindings=()=>({...printBindings,actions(handler){shellAction=handler;}});
printPage.bind();await shellAction('print');
assert.equal(prints,1,'Print / Save PDF must invoke the browser print flow.');
assert.match((await import('node:fs')).readFileSync('src/main/resources/static/css/app.css','utf8'),/@media print.*@page\{size:A4 landscape/s);
printPage.destroy();
console.log('OOP frontend checks passed: escaping, open advising/student dashboard rendering, window banners, forms, roles, lifecycle and action cleanup.');
