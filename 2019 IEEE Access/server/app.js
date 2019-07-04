/**
 * Created by Kim Gyu Hwan on 2017-06-02.
 */
var http=require('http');
var https = require('https')
var express = require('express');
var bodyParser = require('body-parser');
var fs = require('fs');
var app = express();
var robot = require("robotjs");
app.locals.pretty = true;
app.set('view engine','jade'); // jade 와 express 연결
app.set('views','./views');

app.use(express.static('public'));
app.use(bodyParser.urlencoded({extended :false}));
app.use(bodyParser.json());
robot.setMouseDelay(2);

app.post('/form_receiver',function (req, res) {

    var title=req.body.title;
    var description=req.body.description;
    res.send(title+', '+description);
});
app.get('/form_receiver',function (req, res) {
    var title=req.query.title;
    var description=req.query.description;
    res.send(title+', '+description);
});

app.get('/mouse',function (req, res) {
  console.log("x : "+robot.getMousePos().x+", y : "+robot.getMousePos().y);
  var json = formatting_json({}, 200);
  res.end(json);
});

app.post('/start',function (req, res) {
  console.log("start");
 /* robot.moveMouse(1824,923);
  robot.mouseClick();*/
  var json = formatting_json({}, 200);
  res.end(json);
});
app.post('/end',function (req, res) {
  console.log("end");
/*  robot.moveMouse(1814,954);
  robot.mouseClick();
  robot.setMouseDelay(300);*/
  var json = formatting_json({}, 200);
  res.end(json);
});

app.post('/save',function (req, res) {
  var filename=req.body.filename;
  console.log("save : "+filename);
  var json = formatting_json({}, 200);
  res.end(json);
});

function formatting_json(obj, code) {
  var otherObject = obj;
  var json = JSON.stringify({
    result: otherObject,
    code: code
  });
  return json;
}
app.get('/form',function (req, res) {
    res.render('form');
});

app.get('/template',function (req, res) {
  res.render('temp',{time:Date(),_title:'Hello!'});
});
app.get('/',function (req, res) {
  res.redirect('http://klight1994.tistory.com');
//  res.send('Hello World');
});
app.get('/dynamic',function (req, res) {
  var list = '';
  var time = Date();
  for(var i=0;i<5;i++){
    list = list + '<li>coding</li>';
  }
  var output = `
  <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <title>Title</title>
    </head>
     <body>
     Hello, Dynamic!
     <ul>
     ${list}
     </ul>
     ${time}
     </body>
   </html>
`;
  res.send(output);
});

app.get('/test',function (req, res) {
  res.send('Hello Router, <img src="/test.png">');
})
app.get('/login', function (req, res){
  res.writeHead(200, {'Content-Type': 'text/html'});
  res.write('<h3>Login</h3>');
  res.write('<form method="POST" action="/login">');
  res.write('<label name="userId">UserId : </label>')
  res.write('<input type="text" name="userId"><br/>');
  res.write('<label name="password">Password : </label>')
  res.write('<input type="password" name="password"><br/>');
  res.write('<input type="submit" name="login" value="Login">');
  res.write('</form>');
  res.end();
});

app.post('/login', function (req, res){
  var userId = req.body.userId;
  var password = req.body.password

  res.writeHead(200, {'Content-Type': 'text/html'});
  res.write('Thank you, '+userId+', you are now logged in.');
  res.write('<p><a href="/"> back home</a>');
  res.end();
});
app.listen(80,function () {
  console.log('Example app listening on port 8080!');
});
