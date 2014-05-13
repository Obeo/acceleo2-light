

## **Build the acceleo2.light update-site**

If you have Java and Maven, you can build acceleo2.light.

### ** Building for luna **

#### ** Launching the build **

Go to the parent project : 
`cd ./`

In one command line:

`mvn clean package -Pluna`

To launch the tests :

`mvn clean verify -Pluna`

#### ** Build results **

The build produces an update-site containing all the features of the project in :
`./target/repository/`


#### On the Eclipse Build Servers

If you need to sign the build artifacts, use the profile SIGN, for instance: 

`mvn clean verify -Pluna,SIGN`


#### Setting up Jenkins ####

Creating the job

`curl --user USER:PASS -H "Content-Type: text/xml" -s --data-binary "@config.xml" "http://JENKINS_ENDPOINT/createItem?name=acceleo2.light--master"`

Updating the job 

`curl --user USER:PASS -H "Content-Type: text/xml" -s --data-binary "@config.xml" "http://JENKINS_ENDPOINT/job/acceleo2.light--master/config.xml`

