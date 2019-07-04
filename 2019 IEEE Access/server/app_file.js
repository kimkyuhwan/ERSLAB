/**
 * Created by Kim Gyu Hwan on 2017-06-08.
 */
var express =require('express');
var app=express();
var BodyParser=require('body-parser');
var fs=require('fs');

app.locals.pretty=true;
app.set('views','./views_file');
app.set('view engine','jade');

app.use(express.static('public'));
app.use(BodyParser.urlencoded({extended:false}));

app.get(['/', '/:id'],function(req,res){
 fs.readdir('data',function (err, files) {
   if(err){
    console.log('AAAA'+err);
    res.status(500).send('Internal Server Error');
   }
   var id=req.params.id;
   // id값이 있을 때
   if(id=='new'){
    res.render('new',{topics: files,title:'new',desc:""});
   }
   else if(id) {
     fs.readFile('data/' + id, 'utf8', function (err, data) {
       if (err) {
         console.log('BBBB'+err);
         res.status(500).send('Internal Server Error');
       }
       res.render('view', {topics: files, title: id, desc: data});
     });
   }
   else {
     // id값이 없을 때
     res.render('view', {topics: files, title:'Welcome', desc:'Hello, JavaScript for server.'});
   }
 });
});

/*
app.get('/topic/:id',function (req, res) {
  var id=req.params.id;
  fs.readdir('data',function (err, files) {
    if(err){
      console.log(err);
      res.status(500).send('Internal Server Error');
    }
    fs.readFile('data/'+id,'utf8',function (err, data) {
      if(err){
        console.log(err);
        res.status(500).send('Internal Server Error');
      }
      res.render('view', {topics:files, title:id, desc:data});
    });
  });

});*/

app.post('/',function (req, res) {
 var title=req.body.title;
 var description=req.body.description;
 fs.writeFile('data/'+title,description,function(err){
  if(err){
      console.log("CCCC");
   res.status(500).send('Internal Server Error');
  }
   res.redirect('/'+title);
 });
});


app.listen(80, function(){
 console.log("connected 80 port!");
});