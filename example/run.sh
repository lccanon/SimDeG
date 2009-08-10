java -Djava.util.logging.config.file=property/logging.properties -jar jar/simdeg.jar example/conf.txt
java -Djava.util.logging.config.file=property/logging.properties -jar jar/simdeg-rep.jar example/rep.txt
java -ea -Djava.util.logging.config.file=logging.properties -javaagent:/home/canon/opt/local/jip-1.1.1/profile/profile.jar -Dprofile.properties=profile.properties -jar simrep.jar example/conf.txt
java -jar /home/canon/opt/local/jip-1.1.1/client/jipViewer.jar profile.xml &
java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -ea org.junit.runner.JUnitCore simdeg.reputation.TestAgreementReputationSystem
