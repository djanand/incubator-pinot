..
.. Licensed to the Apache Software Foundation (ASF) under one
.. or more contributor license agreements.  See the NOTICE file
.. distributed with this work for additional information
.. regarding copyright ownership.  The ASF licenses this file
.. to you under the Apache License, Version 2.0 (the
.. "License"); you may not use this file except in compliance
.. with the License.  You may obtain a copy of the License at
..
..   http://www.apache.org/licenses/LICENSE-2.0
..
.. Unless required by applicable law or agreed to in writing,
.. software distributed under the License is distributed on an
.. "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
.. KIND, either express or implied.  See the License for the
.. specific language governing permissions and limitations
.. under the License.
..

.. _batch-data-ingestion:

Batch Data Ingestion
====================

In practice, we need to run Pinot data ingestion as a pipeline or a scheduled job.

Assuming `pinot-distribution` is already built, inside `examples` directory, you could find several sample table layouts.

Table Layout
-----------

Usually each table deserves its own directory, like `airlineStats`.

Inside the table directory, `rawdata` is created to put all the input data.

Typically, for data events with timestamp, we partition those data and store them into a daily folder.
E.g. a typically layout is like: `rawdata/%yyyy%/%mm%/%dd%/[daily_input_files]`.


Configuring batch ingestion job
-------------------------------

Create a batch ingestion job spec file to describe how to ingest the data.

Below is an example (also located at `examples/batch/airlineStats/ingestionJobSpec.yaml`)

.. code-block:: none

# jobType: Pinot ingestion job type.
# Supported job types are:
#   'SegmentCreation'
#   'SegmentTarPush'
#   'SegmentUriPush'
#   'SegmentCreationAndTarPush'
#   'SegmentCreationAndUriPush'
jobType: SegmentCreationAndTarPush

# inputDirURI: Root directory of input data, expected to have scheme configured in PinotFS.
inputDirURI: 'examples/batch/airlineStats/rawdata'

# includeFileNamePattern: include file name pattern, supported glob pattern.
# Sample usage:
#   'glob:*.avro' will include all avro files just under the inputDirURI, not sub directories;
#   'glob:**\/*.avro' will include all the avro files under inputDirURI recursively.
includeFileNamePattern: 'glob:**/*.avro'

# excludeFileNamePattern: exclude file name pattern, supported glob pattern.
# Sample usage:
#   'glob:*.avro' will exclude all avro files just under the inputDirURI, not sub directories;
#   'glob:**\/*.avro' will exclude all the avro files under inputDirURI recursively.
# _excludeFileNamePattern: ''

# outputDirURI: Root directory of output segments, expected to have scheme configured in PinotFS.
outputDirURI: 'examples/batch/airlineStats/segments'

# overwriteOutput: Overwrite output segments if existed.
overwriteOutput: true

# pinotFSSpecs: defines all related Pinot file systems.
pinotFSSpecs:

  - # scheme: used to identify a PinotFS.
    # E.g. local, hdfs, dbfs, etc
    scheme: file

    # className: Class name used to create the PinotFS instance.
    # E.g.
    #   org.apache.pinot.spi.filesystem.LocalPinotFS is used for local filesystem
    #   org.apache.pinot.plugin.filesystem.AzurePinotFS is used for Azure Data Lake
    #   org.apache.pinot.plugin.filesystem.HadoopPinotFS is used for HDFS
    className: org.apache.pinot.spi.filesystem.LocalPinotFS

# recordReaderSpec: defines all record reader
recordReaderSpec:

  # dataFormat: Record data format, e.g. 'avro', 'parquet', 'orc', 'csv', 'json', 'thrift' etc.
  dataFormat: 'avro'

  # className: Corresponding RecordReader class name.
  # E.g.
  #   org.apache.pinot.plugin.inputformat.avro.AvroRecordReader
  #   org.apache.pinot.plugin.inputformat.csv.CSVRecordReader
  #   org.apache.pinot.plugin.inputformat.parquet.ParquetRecordReader
  #   org.apache.pinot.plugin.inputformat.json.JsonRecordReader
  #   org.apache.pinot.plugin.inputformat.orc.OrcRecordReader
  #   org.apache.pinot.plugin.inputformat.thrift.ThriftRecordReader
  className: 'org.apache.pinot.plugin.inputformat.avro.AvroRecordReader'

# tableSpec: defines table name and where to fetch corresponding table config and table schema.
tableSpec:

  # tableName: Table name
  tableName: 'airlineStats'

  # schemaURI: defines where to read the table schema, supports PinotFS or HTTP.
  # E.g.
  #   hdfs://path/to/table_schema.json
  #   http://localhost:9000/tables/myTable/schema
  schemaURI: 'http://localhost:9000/tables/airlineStats/schema'

  # tableConfigURI: defines where to reade the table config.
  # Supports using PinotFS or HTTP.
  # E.g.
  #   hdfs://path/to/table_config.json
  #   http://localhost:9000/tables/myTable
  # Note that the API to read Pinot table config directly from pinot controller contains a JSON wrapper.
  # The real table config is the object under the field 'OFFLINE'.
  tableConfigURI: 'http://localhost:9000/tables/airlineStats'

# pinotClusterSpecs: defines the Pinot Cluster Access Point.
pinotClusterSpecs:
  - # controllerURI: used to fetch table/schema information and data push.
    # E.g. http://localhost:9000
    controllerURI: 'http://localhost:9000'

# pushJobSpec: defines segment push job related configuration.
pushJobSpec:

  # pushAttempts: number of attempts for push job, default is 1, which means no retry.
  pushAttempts: 2

  # pushRetryIntervalMillis: retry wait Ms, default to 1 second.
  pushRetryIntervalMillis: 1000

Executing the job
-----------------
Below command will create example table into Pinot cluster.

.. code-block:: bash

   bin/pinot-admin.sh AddTable  -schemaFile examples/batch/airlineStats/airlineStats_schema.json -tableConfigFile examples/batch/airlineStats/airlineStats_offline_table_config.json -exec

Below command will kick off the ingestion job to generate Pinot segments and push them into the cluster.

.. code-block:: bash

   bin/pinot-ingestion-job.sh examples/batch/airlineStats/ingestionJobSpec.yaml

After job finished, segments are stored in ` examples/batch/airlineStats/segments` following same layout of input directory layout.