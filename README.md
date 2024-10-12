This is a Kotlin Multiplatform project targeting Server.

* `/server` is for the Ktor server application.
* Configure Authorization  credentials
  * [Follow these instructions](https://ktor.io/docs/server-oauth.html#authorization-credentials)
* Add Environment Variables
  * GOOGLE_CLIENT_ID
  * GOOGLE_CLIENT_SECRET
* Run the gradle project
* Access "http://localhost:8080/debug/session" via browser
  * Expect Sign-On experience via Google
  * Be redirected to a page outputting
    * A `state` var
    * An auth token
    * Your Google account ID
* Load Insomnia and send a GET request:
  * URL: http://localhost:8080/debug/session
  * Expect Sign-On experience via Google
  * Experience an error scenario from Google sign-on
