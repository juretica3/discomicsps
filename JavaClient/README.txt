DAVID Web Service client using Java
=============================================
The sample client files contain Java source code showing you how to connect DAVID Web service
and generate your reports in no time. 

1. Download  JavaClient-1.1.zip from DAVID webservice site:
       http://david.abcc.ncifcrf.gov/webservice/sample_clients/JavaClient-1.1.zip

2. Extract the downloaded zip file to your working directory: $JavaClient

3. Go to $JavaClient directory and compile the client java files by typing the following command line: 
	"javac -extdirs lib sample\session\client\ChartReportClient.java"
   Run the client class file by typing the following command line:
   	"java -Djava.ext.dirs=lib  sample.session.client.ChartReportClient"
   Check the generated text report file in your working directory

4. Edit ChartReportClient.java to make your own client java files in $JavaClient\sample\session\client\

5. Compile and run your own client java files in your $JavaClient directory 