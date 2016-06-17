$(function(){$("#login_modal").on("shown.bs.modal",function(){$("#name").focus()});$("a[data-toggle='tab']").on("shown.bs.tab",function(a){$("#chat_text").focus();$("#chat_div").scrollTop($("#chat_div")[0].scrollHeight);$("#con_text").focus();$("#con_div").scrollTop($("#con_div")[0].scrollHeight)});$(window).resize(function(){var a=$(window).height()-110;$("#chat_div").css("height",a);$("#con_div").css("height",a)}).resize();$("#login_modal").modal({backdrop:"static",keyboard:false});webSocket=new WebSocket("ws://"+location.host+"/LinmaluWebController");webSocket.onopen=function(){log("LinmaluWebController : 연결 성공")};webSocket.onclose=function(){log("LinmaluWebController : 연결 종료");alert("LinmaluWebController : 서버와 연결이 종료되었습니다.")};webSocket.onmessage=function(b){var a=JSON.parse(GibberishAES.aesDecrypt(b.data,key));switch(a.type){case"con":receiveMessage($("#con_div"),a.data);break;case"chat":receiveMessage($("#chat_div"),a.data);break}}});function login(){var b=$("#name");var d=$("#pass");var c=$("#name_group");var a=$("#pass_group");if(b.val().length<1){c.removeClass("has-success");c.addClass("has-error");b.focus()}else{if(d.val().length<1){c.removeClass("has-error");c.addClass("has-success");a.removeClass("has-success");a.addClass("has-error");d.focus()}else{a.removeClass("has-error");a.addClass("has-success");$("#login_modal").modal("hide");$("#chat_text").focus();key=CryptoJS.PBKDF2(d.val(),CryptoJS.SHA256(d.val()).toString(),{keySize:2,iterations:1024});GibberishAES.size(128);webSocket.send(JSON.stringify({name:b.val(),data:GibberishAES.aesEncrypt("LinmaluWebController",key)}))}}return false}function chat(){sendMessage("chat");return false}function console(){sendMessage("con");return false}function sendMessage(a){var b=$("#"+a+"_text");if(b.val().length>0){webSocket.send(GibberishAES.aesEncrypt(JSON.stringify({type:a,data:b.val()}),key));b.val("")}}function log(a){receiveMessage($("#chat_div"),a);receiveMessage($("#con_div"),a)}function receiveMessage(c,a){var b=false;if(c[0].scrollHeight-c.scrollTop()<=c.outerHeight()){b=true}c.append("<br>"+a);if(b){c.scrollTop(c[0].scrollHeight)}};