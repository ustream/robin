Robin
-----
Parallel test execution with a Robotium RC based android automation framework.
This project uses testNG + ReportNG (http://testng.org) to run tests and
ivy + ant to handle dependencies and build.

#### Prerequisites: ####
* installed ant (ANT_HOME env. variable set and added to the PATH)
* installed android-sdk (ANDROID_HOME env. variable set and added to the PATH)
* JDK 1.6 installed (JAVA_HOME env. variable set and added to the PATH)
* internet connection for ivy dependency
* device in debug mode

#### Run sample test ####
There are simple calculator multiply tests to demonstrate Robin functionalaity. How can you run? Follow the instructions.
Clone our repository.

Open terminal and do the following where X is the sdk target id with version >=10:

    cd RobinTests 
    ant buildTestsuite


    cd SAFSTCPMessenger
    android update project --target X --path .
    ant debug


    cd RobotiumTestRunner
    android update project --target X --path .
    ant debug

In the RobinTests directory call:

<code>ant runtests -DtesngXml="SmokeTests.xml"</code>
where runtests is the ant command to run the selected tests, that are defined in SmokeTests.xml

to run multiple suites parallel add more XMLs to -DtestngXml parameter:

<code>ant runtests -DsuiteThreadPoolSize=2 -DtestngXml="SmokeTests.xml TestParallel.xml"</code>

to run with minimum device number use (the default mindevice is 1):

<code>ant runtests -Dmindevice=2</code>

You can run  tests on required devices, e.g.: you can run a test only Samsung mobile. You can set this in  testngXml:

<code>parameter name="device" value=".Samsung" </code>
<p>If you don't need to separete deveices, you sholud use <code>value=".*"</code></p>

#### Create your project ####
If you could succesfully run the sample tests, you should create your own test project. The easiest way to do this to copy the sample Project and make your changes on that.
Depending on the IDE you use, this may require different steps, but Eclipse project settings are included in the repository.
To write your tests in Eclipse, you can use import -> existing java project and selecting the project folders. The required Eclipse plugins include the ADT (http://developer.android.com/sdk/installing/installing-adt.html) and IvyDE (http://ant.apache.org/ivy/ivyde/download.cgi) plugins.
After you made a fresh copy from the RobinTests sample project...
Edit the build.xml and change the project specific values:

    <!-- Set project name according to your test project  -->
    <project name="MyProject" default="runtests" xmlns:ivy="antlib:org.apache.ivy.ant">
    <!-- Set version to be used as ivy version -->
    <property name="version" value="0.1" />
    <!-- Review the relative path to the Robin folder -->
    <property name="robin.dir" value="${basedir}${file.separator}..${file.separator}Robin" />


Please be aware that ant tasks are responsible for all the building and lolcal repository publishing purposes. Also the output directory with all previous result will be deleted by the ant.
Thus you may also review the ivy.xml in your project directory to define the module name, and use the ant project ("MyProject") as the artifact name:

    <info organisation="com.robin" module="AndroidTests" />
    <publications>
    <artifact name="RobinAndroid" type="jar" ext="jar" />
    </publications>

You must also set the content of the Robin.properties file:
<ul>
 <li>config.dir=#basedir#/conf</li>
 <li>tlxmls.dir=#basedir#/tlxmls</li>
 <li>robin.device=.*</li>
 <li>robin.autFile=./AndroidCalculator.apk</li>
 <li>robin.autSource=(only if you use resource)</li>
 <li>robin.messengerSource=.\\..\\SAFSTCPMessenger</li>
 <li>robin.testRunnrerSource=.\\..\\RobotiumTestRunner</li>
 <li>robin.resignedAutPath=build\\aut</li>
 <li>robin.logfile=build\\log.txt</li>
 <li>robin.debug=true</li>
 <li>mindevice=2</li>
</ul>
The above values are:
-   config.dir Defines the folder where a robinConfig.properties can be placed to store test related values.
-   tlxmls.dir The folder where you can create your translation xml-s
-   robin.device A regexp selector that filters the connected devices to use for test run
-   robin.autFile The path to the application under test .apk file
-   robin.autSource The path to the source folder of the application under test. Only needed if you use resource based locators or class references from the application source code. (You may require to costumize the buildAUTReferences ant task according to the application project structure).
-   robin.messengerSource The relative path to the SAFSTCPMessenger folder.
-   robin.testRunnrerSource The relative path to the RobotiumTestRunner folder.
-   robin.resignedAutPath The path where all the resigned aut .apk files, runtime buildt messenger and test runner .apk files saved at.
-   robin.logfile The file path where the System.out is redirected to.
-   robin.debug Boolean flag whether the communication protocol should log the sent and received messages.
-   mindevice The number of connected devices to wait for before starting tests.

The test parallelism and running is controlled by the testng xml found in the config.dir/testng folder. There are some custom parameters that can be defined in these xmls:

    <parameter name="aut" value=".\\AndroidCalculator.apk" />
    <parameter name="device" value=".*" />

The  descending order of settings priority is:
testng xml parameter > build.properties(local file) > Robin.properies > ant target parameters

Because runtime the test compiles projects according to the aut .apk manifest file, on one computer only one instance of robin should run.

One of the main features of this test framework is the possibility to run UI tests using two or more involved devices. You can use the <code>@Multidevice</code> annotation on a test method to achive this.

#### Report ####

At the end of the test run all results are saved in the build folder of your project: reports, screenshots, used .apk files etc.
HTML report: ...\build\reports\testNG\html\index.html, if test failed, the screenshot and log cat informations will be saved.
