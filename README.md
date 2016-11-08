# Example related to count friends 

curl -s https://immense-river-17982.herokuapp.com/?since=1352102565590 | head -5

{"from":{"id":"vFGj1gT4c6I=","name":"Christopher Newman"},"to":{"id":"V+HGOZuQQZY=","name":"Jeff Nieves"},"areFriends":true,"timestamp":"1352102565590"}
{"from":{"id":"XJ2CG/o6280=","name":"Alfred Blair"},"to":{"id":"Darv88kRhV4=","name":"ken Downs"},"areFriends":false,"timestamp":"1352102565591"}
{"from":{"id":"exffc8SJ36Y=","name":"John Williams"},"to":{"id":"nZW7lq6X1kc=","name":"Julio Miranda"},"areFriends":true,"timestamp":"1352102565592"}
{"from":{"id":"G2O+uLnMRtI=","name":"Floyd Nguyen"},"to":{"id":"U17kUNsYGhA=","name":"Shawn Atkinson"},"areFriends":true,"timestamp":"1352102565593"}
{"from":{"id":"PIAg6tkDEVA=","name":"Anthony Michael"},"to":{"id":"MARqIlKVT7c=","name":"Angel Santana"},"areFriends":true,"timestamp":"1352102565594"}

So from the first line, Christopher became friends with Jeff.  Then Alfred "unfriended" Ken, and so on.

Write a new http API in the language of your choice that consumes this existing API.  I should be able to make a call to your API with a user's id, and get back a list of who their current friends are."

