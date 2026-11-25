import assert from 'node:assert/strict';
import {PasswordSetupForm} from '../src/main/resources/static/js/components/PasswordSetupForm.js';
import {LoginPage} from '../src/main/resources/static/js/pages/LoginPage.js';

class Field extends EventTarget {
    value='';textContent='';hidden=false;disabled=false;
    focus(){}
}
const fields=Object.fromEntries(['#password-setup-form','#setup-id','#setup-password','#setup-confirm','#setup-error','#back-to-login','#submit'].map(id=>[id,new Field()]));
fields['#password-setup-form'].reset=()=>{for(const id of ['#setup-id','#setup-password','#setup-confirm'])fields[id].value='';};
fields['#password-setup-form'].querySelector=()=>fields['#submit'];
globalThis.document={querySelector:selector=>fields[selector]};
const calls=[],completed=[];
let fail=false;
const api={async post(path,body){calls.push({path,body});if(fail)throw new Error('This ID is not eligible for first-time password setup. Contact the administrator.');return{message:'Password set successfully. You can now log in.'};}};
const component=new PasswordSetupForm(api,(id,message)=>completed.push({id,message}));
component.bind();component.open('S001');
assert.equal(fields['#setup-id'].value,'S001');
assert.equal(fields['#password-setup-form'].hidden,false);
fields['#setup-password'].value='personal-password';fields['#setup-confirm'].value='different-password';
fields['#password-setup-form'].dispatchEvent(new Event('submit',{cancelable:true}));
await new Promise(resolve=>setTimeout(resolve,0));
assert.equal(calls.length,0);assert.equal(fields['#setup-error'].textContent,'Passwords do not match.');
fail=true;fields['#setup-confirm'].value='personal-password';
fields['#password-setup-form'].dispatchEvent(new Event('submit',{cancelable:true}));
await new Promise(resolve=>setTimeout(resolve,0));
assert.equal(completed.length,0);assert.equal(fields['#password-setup-form'].hidden,false);assert.equal(fields['#submit'].disabled,false);
fail=false;fields['#setup-id'].value=' S001 ';
fields['#password-setup-form'].dispatchEvent(new Event('submit',{cancelable:true}));
await new Promise(resolve=>setTimeout(resolve,0));
assert.equal(calls.at(-1).path,'/auth/setup-password');assert.equal(calls.at(-1).body.id,'S001');
assert.equal(completed[0].id,'S001');assert.equal(fields['#password-setup-form'].hidden,true);
assert.equal(fields['#setup-password'].value,'');assert.equal(fields['#setup-confirm'].value,'');
// Check the login page's first-time/back navigation without a browser or network.
for(const key of Object.keys(fields))fields[key]=new Field();
for(const key of ['#login-form','#login-id','#login-password','#login-error','#login-notice','#show-password-setup'])fields[key]=new Field();
fields['#password-setup-form'].reset=()=>{for(const id of ['#setup-id','#setup-password','#setup-confirm'])fields[id].value='';};
fields['#password-setup-form'].querySelector=()=>fields['#submit'];
fields['#login-id'].value='S001';fields['#login-password'].value='not-to-be-kept';
const loginPage=new LoginPage();loginPage.bind();
fields['#show-password-setup'].dispatchEvent(new Event('click'));
assert.equal(fields['#login-form'].hidden,true);assert.equal(fields['#setup-id'].value,'S001');assert.equal(fields['#login-password'].value,'');
fields['#setup-id'].value='S002';fields['#setup-password'].value='not-to-be-kept';
fields['#back-to-login'].dispatchEvent(new Event('click'));
assert.equal(fields['#password-setup-form'].hidden,true);assert.equal(fields['#login-form'].hidden,false);assert.equal(fields['#login-id'].value,'S002');assert.equal(fields['#setup-password'].value,'');
loginPage.destroy();
console.log('First-time password form checks passed: ID prefill, login navigation, mismatch, server failure, success and credential cleanup.');
