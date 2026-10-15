# Website + Admin deploy (Firebase Hosting + Firestore)
1. Firebase Console > Authentication > Sign-in method > Email/Password ON.
   Users tab e duita user banan:  app@smmmoon.com  ar  admin@smmmoon.com  (nijer password din)
   (onno email chaile firestore.rules er upor er duita email ta bodlan, SmsReceiver.java te APP_EMAIL bodlan)
2. Terminal:  npm i -g firebase-tools  ->  firebase login  ->  firebase deploy
   Ei ek command e Firestore database, rules ar website/admin deploy hoye jabe.
   (Database create e error dile Console > Firestore Database > Create database (Native) kore abar deploy)
3. Website: https://bkash-nagad-sms-checker.web.app   (amount: ?amount=500)
   Admin:   https://bkash-nagad-sms-checker.web.app/admin.html
4. "transactions" collection ta prothom SMS ashar sathe sathe nijei toiri hobe.
