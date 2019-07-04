/**
 * Created by Kim Gyu Hwan on 2017-06-08.
 */

var port=80;

var express =require('express');
var app=express();
var BodyParser=require('body-parser');
var fs=require('fs');
var OrientDB = require('orientjs');
var server= OrientDB({
  host:'localhost',
  port:2424,
  username:'root',
  password:'@a315248'
});

var db=server.use('o3');

app.locals.pretty=true;
app.set('views','./views_orientdb');
app.set('view engine','jade');

app.use(express.static('public'));
app.use(BodyParser.urlencoded({extended:false}));

app.get('/:id/delete',function (req, res) {
  var sql='SELECT FROM topic';
  var id= req.params.id;

  db.query(sql).then(function (topics) {
    var sql='SELECT FROM topic WHERE @rid=:rid';
    db.query(sql,{params:{rid:id}}).then(function (topic) {
      res.render('delete', {topics: topics,topic:topic[0]});
    });

  });
});

app.post('/:id/delete',function (req, res) {
  var sql="DELETE FROM topic WHERE @rid=:rid";
  var id= req.params.id;
  db.query(sql,{params:{rid:id}}).then(function (results) {
    console.log(results);
    res.redirect('/');
  })
});
app.get('/:id/edit',function (req, res) {
  var sql='SELECT FROM topic';
  var id= req.params.id;

  db.query(sql).then(function (topics) {
    var sql='SELECT FROM topic WHERE @rid=:rid';
    db.query(sql,{params:{rid:id}}).then(function (topic) {
      res.render('edit', {topics: topics,topic:topic[0]});
    });

  });
});
app.post('/:id/edit',function (req, res) {
  var sql="UPDATE topic SET title=:title, description=:desc, author=:author WHERE @rid=:rid";
  var id= req.params.id;
  var newtitle=req.body.title;
  var newdescription=req.body.description;
  var newauthor=req.body.author;

  db.query(sql,{params:{rid:id,title:newtitle,desc:newdescription,author:newauthor}}).then(function (results) {
    var rid=encodeURIComponent(id);
    res.redirect('/'+rid);
  });
});


app.get(['', '/:id'],function(req,res){
  var sql='SELECT FROM topic';
  var id= req.params.id;

    db.query(sql).then(function (topics) {
      if(id=='add'){
        res.render('add',{topics:topics});
      }
      else if(id) {
        var sql = 'SELECT FROM topic WHERE @rid=:rid';

        db.query(sql, {params: {rid: id}}).then(function (topic) {
          res.render('view', {topics: topics, topic: topic[0]});
        });
      }
      else{
        res.render('view', {topics: topics});
      }
    });

});

app.post('/add',function (req, res) {
  var title=req.body.title;
  var description=req.body.description;
  var author=req.body.author;
  var sql="INSERT INTO topic (title,description,author) VALUES (:title, :desc, :author)";
  db.query(sql,{params:{title:title,desc:description,author:author}}).then(function (results) {
    console.log(results);
    var rid=encodeURIComponent(results[0]['@rid']);
    res.redirect('/'+rid);
  })
});


app.listen(port, function(){
  console.log("connected "+port+" port!");
});