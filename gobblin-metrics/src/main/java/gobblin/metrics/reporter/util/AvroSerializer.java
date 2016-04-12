/*
 * Copyright (C) 2014-2016 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 */

package gobblin.metrics.reporter.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.avro.Schema;
import org.apache.avro.io.Encoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closer;


/**
 * Class used for serializing Avro {@link org.apache.avro.specific.SpecificRecord} for metrics reporting.
 *
 * @param <T> Type of avro records that will be serialized.
 */
public abstract class AvroSerializer<T extends SpecificRecord> implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(AvroSerializer.class);

  private final Closer closer;

  private final Encoder encoder;
  private final ByteArrayOutputStream byteArrayOutputStream;
  private final DataOutputStream out;
  private final SpecificDatumWriter<T> writer;
  private SchemaVersionWriter schemaVersionWriter;

  public AvroSerializer(Schema schema, SchemaVersionWriter schemaVersionWriter) throws IOException {
    this.closer = Closer.create();

    this.byteArrayOutputStream = new ByteArrayOutputStream();
    this.out = this.closer.register(new DataOutputStream(this.byteArrayOutputStream));
    this.encoder = getEncoder(schema, this.out);
    this.schemaVersionWriter = schemaVersionWriter;
    this.writer = new SpecificDatumWriter<>(schema);

  }

  /**
   * Change the {@link gobblin.metrics.reporter.util.SchemaVersionWriter} used by this serializer.
   * @param schemaVersionWriter new {@link gobblin.metrics.reporter.util.SchemaVersionWriter} to use.
   */
  public void setSchemaVersionWriter(SchemaVersionWriter schemaVersionWriter) {
    this.schemaVersionWriter = schemaVersionWriter;
  }

  /**
   * Get {@link org.apache.avro.io.Encoder} for serializing Avro records.
   * @param schema {@link org.apache.avro.Schema} that will be written to outputStream.
   * @param outputStream {@link java.io.OutputStream} where records should be written.
   * @return Encoder.
   */
  protected abstract Encoder getEncoder(Schema schema, OutputStream outputStream) throws IOException;

  /**
   * Converts a {@link gobblin.metrics.MetricReport} to bytes to send through Kafka.
   *
   * <p>
   *  Actual serialized record will be prepended with a schema version generated by {@link #schemaVersionWriter}.
   * </p>
   *
   * @param record MetricReport to serialize.
   * @return Serialized bytes.
   */
  public synchronized byte[] serializeRecord(T record) {

    try {
      this.byteArrayOutputStream.reset();
      // Write schema versioning information.
      this.schemaVersionWriter.writeSchemaVersioningInformation(record.getSchema(), this.out);
      // Now write the record itself.
      this.writer.write(record, this.encoder);
      this.encoder.flush();
      return this.byteArrayOutputStream.toByteArray();
    } catch (IOException exception) {
      LOGGER.warn("Could not serialize Avro record for Kafka Metrics.", exception);
      return null;
    }
  }

  @Override
  public void close() throws IOException {
    this.closer.close();
  }
}
