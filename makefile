install:
	mvn install
	cp -R ~/.m2/repository/com/thechoppergroup/spark-core ${CHOPPER_HOME}/lib/
