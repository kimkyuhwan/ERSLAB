/**
 * Created by erslab-gh on 2017-06-08.
 */
var OrientDB = require('orientjs');

var server= OrientDB({
    host:'localhost',
    port:2424,
    username:'root',
    password:'@a315248'
});

var db=server.use('o3');
/*
console.log('Using Database : '+db.name);
db.record.get('#42:0').then(function (err,record) {
    if(err){
        console.log(err);
    }
    console.log('Loaded record:',record);
});
*/

// SELECT
/*
var sql='SELECT * FROM topic WHERE title=:title';
var param = {
    params: {
        rid: '#33:0',
        title:'NPM'
    }
};
console.log(sql);
db.query(sql,param).then(function (results) {
    console.log(results);
});
*/

// INSERT
/*
var sql="INSERT INTO topic (title,description) VALUES (:title, :desc)";
db.query(sql,{params:{
    title:'songju',
        desc:'짱짱'
}}).then(function (results) {
    console.log(results);
});

// return 삽입된 결과
*/

// UPDATE
/*
var sql="UPDATE topic SET title=:title WHERE @rid=:rid";
var param={
    params:{
        title:'change',
        rid:'#36:0'
    }
};

db.query(sql,param).then(function (results) {
    console.log(results);
});
// return 삽입된 행의 수
*/

// DELETE
var sql="DELETE FROM topic WHERE @rid=:rid";
db.query(sql,{params:{rid:'#36:0'}}).then(function (results) {
    console.log(results);
});
// return 삭제된 행의 수