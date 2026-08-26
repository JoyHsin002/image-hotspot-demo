export async function api(url,options={}){const r=await fetch(url,options);if(!r.ok){let x;try{x=await r.json()}catch{x={}}throw new Error(x.message||`请求失败 (${r.status})`)}return r.status===204?null:r.json()}
export function toast(message,error=false){const e=document.createElement('div');e.className='toast'+(error?' error':'');e.textContent=message;document.body.append(e);setTimeout(()=>e.remove(),2600)}
export function esc(s=''){return String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
