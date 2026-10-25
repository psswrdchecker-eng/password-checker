<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>GuessWise: password checker</title>
<style>
:root{box-sizing:border-box;padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px);
--bg:#fff;--card:#fbeaea;--ink:#262626;--mute:#595959;--red:#c00000;--line:#e3c9c9;--ok:#1e8449;--warn:#b36b00}
:root:not([data-theme="light"]){}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#161212;--card:#241a1a;--ink:#f2eded;--mute:#b9aeae;--red:#ff6b6b;--line:#3d2a2a;--ok:#4cc27f;--warn:#e0a040}}
:root[data-theme="dark"]{--bg:#161212;--card:#241a1a;--ink:#f2eded;--mute:#b9aeae;--red:#ff6b6b;--line:#3d2a2a;--ok:#4cc27f;--warn:#e0a040}
html{scroll-padding-top:env(safe-area-inset-top,0px)}
*,*:before,*:after{box-sizing:inherit}
body{margin:0;background:var(--bg);color:var(--ink);font:16px/1.5 Calibri,"Segoe UI",system-ui,sans-serif}
main{max-width:720px;margin:0 auto;padding:32px 20px 48px}
h1{font-size:2rem;margin:0 0 4px}h1 span{color:var(--red)}
.sub{color:var(--mute);margin:0 0 24px}
.card{background:var(--card);border-radius:14px;padding:20px;margin-bottom:16px}
label{display:block;font-weight:700;font-size:.85rem;margin-bottom:6px}
.row{display:flex;gap:8px}
input{width:100%;font:inherit;padding:12px;border:1px solid var(--line);border-radius:10px;background:var(--bg);color:var(--ink)}
input:focus{outline:2px solid var(--red)}
button{font:inherit;font-weight:700;border:0;border-radius:10px;padding:0 16px;background:var(--red);color:#fff;cursor:pointer;white-space:nowrap}
button.ghost{background:transparent;color:var(--red);border:1px solid var(--red);padding:10px 16px}
.bar{display:grid;grid-template-columns:repeat(4,1fr);gap:6px;margin:16px 0 8px}
.bar i{height:8px;border-radius:4px;background:var(--line)}
.verdict{font-size:1.4rem;font-weight:700;margin:0}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:12px;margin-top:14px}
.stat{background:var(--bg);border-radius:10px;padding:12px}
.stat b{display:block;font-size:.75rem;color:var(--mute);text-transform:uppercase;letter-spacing:.04em}
.stat span{font-weight:700;word-break:break-all}
ul{margin:8px 0 0;padding-left:20px}
.ctx{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px}
small,.note{color:var(--mute);font-size:.85rem}
code{font-family:Consolas,monospace}
</style>
</head>
<body>
<main>
<h1>Guess<span>Wise</span></h1>
<p class="sub">A password checker that thinks like an attacker. Everything runs in your browser; nothing you type is sent anywhere.</p>

<div class="card">
<label for="pw">Password to test</label>
<div class="row"><input id="pw" type="password" autocomplete="off" spellcheck="false" placeholder="Type or paste a password"><button id="tog" type="button">Show</button></div>
<div class="bar" id="bar"><i></i><i></i><i></i><i></i></div>
<p class="verdict" id="verdict">Waiting for input</p>
<p class="note" id="reason">Length matters more than symbols. Try a few unrelated words.</p>
<ul id="tips"></ul>
<div class="grid">
<div class="stat"><b>Offline cracking time</b><span id="crack">-</span></div>
<div class="stat"><b>Length</b><span id="len">0</span> <small>(NIST: 15+ if password is the only factor)</small></div>
<div class="stat"><b>Breach lookup prefix</b><span id="prefix">-</span></div>
</div>
<p class="note" style="margin-top:12px">The prefix is the only part of the SHA-1 hash a k-anonymity breach lookup would send. This hosted page cannot call the Have I Been Pwned API, so it checks against a built-in list of very common passwords instead.</p>
</div>

<div class="card">
<label>Optional context (makes the check stricter)</label>
<div class="ctx">
<input id="c1" placeholder="Your name" aria-label="Your name">
<input id="c2" placeholder="Email" aria-label="Email">
<input id="c3" placeholder="Site or app name" aria-label="Site name">
</div>
</div>

<div class="card">
<label>Need a strong one?</label>
<div class="row"><input id="gen" readonly aria-label="Generated password"><button id="gbtn" type="button">Generate</button></div>
<p class="note">20 random characters from your browser's secure random generator. Store it in a password manager.</p>
<button class="ghost" id="use" type="button">Test this one</button>
</div>
<p class="note">Scoring uses zxcvbn, which estimates guessability from patterns (dictionary words, l33t swaps, keyboard walks, dates, repeats). Always pair a good password with MFA or a passkey.</p>
</main>

<script src="https://cdn.jsdelivr.net/npm/zxcvbn@4.4.2/dist/zxcvbn.js"></script>
<script>
(function(){
var $=function(i){return document.getElementById(i)};
var COMMON="123456 123456789 12345678 password password1 password123 qwerty qwerty123 111111 12345 iloveyou admin welcome letmein monkey dragon abc123 123123 1234567890 000000 football baseball master sunshine princess login passw0rd starwars trustno1 654321 superman 1q2w3e4r zaq12wsx qazwsx google changeme".split(" ");
var cols=["#c00000","#c00000","#b36b00","#1e8449","#1e8449"];
var names=["Very weak","Weak","Fair","Strong","Very strong"];
var timer=0;
function sha1(s){return crypto.subtle.digest("SHA-1",new TextEncoder().encode(s)).then(function(b){return Array.prototype.map.call(new Uint8Array(b),function(x){return x.toString(16).padStart(2,"0")}).join("").toUpperCase()})}
function ctx(){var a=[$("c1").value,$("c2").value,$("c3").value],o=[];a.forEach(function(v){v=v.trim();if(!v)return;o.push(v);v.split(/[\s@._-]+/).forEach(function(p){if(p.length>2)o.push(p)})});return o}
function setBar(n,c){var b=$("bar").children;for(var i=0;i<4;i++)b[i].style.background=(n>0&&i<Math.max(n,1))?c:""}
function fallback(p){var s=Math.min(4,Math.floor(p.length/5));return{score:s,crack:"n/a",tips:[],warn:""}}
function run(){
 var p=$("pw").value;$("len").textContent=p.length;
 if(!p){setBar(0);$("verdict").textContent="Waiting for input";$("verdict").style.color="";$("reason").textContent="Length matters more than symbols. Try a few unrelated words.";$("tips").innerHTML="";$("crack").textContent="-";$("prefix").textContent="-";return}
 var r,tips=[],warn="";
 if(window.zxcvbn){var z=zxcvbn(p.slice(0,100),ctx());r={score:z.score,crack:z.crack_times_display.offline_fast_hashing_1e10_per_second};warn=z.feedback.warning;tips=z.feedback.suggestions}else{r=fallback(p)}
 var score=r.score,blocked=COMMON.indexOf(p.toLowerCase())>-1,short=p.length<8;
 if(blocked){score=0;warn="This is one of the most common passwords in the world."}
 else if(short){score=Math.min(score,1);tips=["Use at least 8 characters, ideally 15 or more."].concat(tips)}
 else if(p.length<15&&score>=3){tips=tips.concat(["NIST now recommends 15+ characters when a password is the only login factor."])}
 var c=cols[score];setBar(score+1>4?4:score+1,c);
 $("verdict").textContent=blocked?"Blocked: too common":names[score];$("verdict").style.color=c;
 $("reason").textContent=warn||(score>=3?"Hard to guess. Make sure it is unique to this account.":"An attacker's wordlist would find this quickly.");
 $("tips").innerHTML=tips.map(function(t){return "<li>"+t.replace(/[<>&]/g,"")+"</li>"}).join("");
 $("crack").textContent=r.crack;
 sha1(p).then(function(h){if($("pw").value===p)$("prefix").textContent=h.slice(0,5)+"\u2026"})
}
function gen(){var A="abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%^&*-_=+",o="",a=new Uint32Array(20);crypto.getRandomValues(a);for(var i=0;i<20;i++)o+=A[a[i]%A.length];$("gen").value=o}
$("pw").addEventListener("input",run);["c1","c2","c3"].forEach(function(i){$(i).addEventListener("input",run)});
$("tog").onclick=function(){var s=$("pw").type==="password";$("pw").type=s?"text":"password";this.textContent=s?"Hide":"Show"};
$("gbtn").onclick=gen;$("use").onclick=function(){if($("gen").value){$("pw").value=$("gen").value;run()}};
gen();
})();
</script>
</body>
</html>