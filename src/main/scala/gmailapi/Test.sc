package gmailapi

import gmailapi.resources._
import gmailapi.methods._
import gmailapi.resources.MessageFactory

import javax.mail.internet.MimeUtility
import javax.mail.internet.MimeMessage

object Test {

  
	val text = MimeUtility.decodeText("VGVzdCAxMjMNCg==")
                                                  //> text  : String = VGVzdCAxMjMNCg==

  
	val message = MessageFactory.createMessage(
	  labelIds = Seq("INBOX", "SENT"),
	  fromAddress = Some(("Scala API Test", "scala.api.test@gmail.com")),
	  to =  Seq(("Scala API Test", "scala.api.test+foo@gmail.com")),
	  subject = Some("Test 123"),
	  textMsg = Some("Test 123"),
	  htmlMsg = Some("<html>Test 123</html>")
	  )                                       //> message  : gmailapi.resources.Message = Message(None,None,List(INBOX, SENT),
                                                  //| None,None,None,None,Some(RGF0ZTogVHVlLCAyNiBBdWcgMjAxNCAxMzoyMDozNSAtMDQwMCA
                                                  //| oRURUKQ0KRnJvbTogU2NhbGEgQVBJIFRlc3QgPHNjYWxhLmFwaS50ZXN0QGdtYWlsLmNvbT4NClR
                                                  //| vOiBTY2FsYSBBUEkgVGVzdCA8c2NhbGEuYXBpLnRlc3QrZm9vQGdtYWlsLmNvbT4NCk1lc3NhZ2U
                                                  //| tSUQ6IDw5MTkxMzU1NTcuMi4xNDA5MDczNjM1Mjk3LkphdmFNYWlsLm5lbWFuamFAbmVtYW5qYS1
                                                  //| tYnAubG9jYWw-DQpTdWJqZWN0OiBUZXN0IDEyMw0KTUlNRS1WZXJzaW9uOiAxLjANCkNvbnRlbnQ
                                                  //| tVHlwZTogbXVsdGlwYXJ0L21peGVkOyANCglib3VuZGFyeT0iLS0tLT1fUGFydF8wXzUxODU3NjU
                                                  //| 0OS4xNDA5MDczNjM1MTE4Ig0KDQotLS0tLS09X1BhcnRfMF81MTg1NzY1NDkuMTQwOTA3MzYzNTE
                                                  //| xOA0KQ29udGVudC1UeXBlOiBtdWx0aXBhcnQvYWx0ZXJuYXRpdmU7IA0KCWJvdW5kYXJ5PSItLS0
                                                  //| tPV9QYXJ0XzFfNzcwNDQzMzk1LjE0MDkwNzM2MzUxMjUiDQoNCi0tLS0tLT1fUGFydF8xXzc3MDQ
                                                  //| 0MzM5NS4xNDA5MDczNjM1MTI1DQpDb250ZW50LVR5cGU6IHRleHQvcGxhaW47IGNoYXJzZXQ9dXM
                                                  //| tYXNjaWkNCkNvbnRlbnQtVHJhbnNmZXItRW5jb2Rpbmc6IDdiaXQNCg0KVGVzdCAxMjMNCi0tLS0
                                                  //| tLT1fUGFydF8xXzc3MDQ0MzM
                                                  //| Output exceeds cutoff limit.
	  
	  
	  
	  
}