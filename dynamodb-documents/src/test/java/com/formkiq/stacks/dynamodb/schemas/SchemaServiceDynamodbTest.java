/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.stacks.dynamodb.schemas;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.stacks.dynamodb.attributes.AttributeType;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit Tests for {@link SchemaServiceDynamodb}.
 */
@ExtendWith(DynamoDbExtension.class)
public class SchemaServiceDynamodbTest {

  /** {@link SchemaService}. */
  private static SchemaService service;
  /** {@link AttributeService}. */
  private static AttributeService attributeService;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {
    DynamoDbConnectionBuilder db = DynamoDbTestServices.getDynamoDbConnection();
    DynamoDbService dbb = new DynamoDbServiceImpl(db, "Documents");
    service = new SchemaServiceDynamodb(dbb);
    attributeService = new AttributeServiceDynamodb(dbb);
  }

  private static Collection<ValidationError> setSitesSchema(final String siteId,
      final SchemaAttributes schemaAttributes) {
    String name = "somesetschema";
    Schema schema = new Schema().name(name).attributes(schemaAttributes);
    String schemaJson = new GsonBuilder().create().toJson(schema);
    return service.setSitesSchema(siteId, name, schemaJson, schema);
  }

  // TODO remove "name" parameter once 1.15.1 is merged
  private static ClassificationRecord setClassification(final String siteId,
      final String classificationId, final String name, final SchemaAttributes schemaAttributes)
      throws ValidationException {
    Schema schema = new Schema().name(name).attributes(schemaAttributes);
    String schemaJson = new GsonBuilder().create().toJson(schema);
    return service.setClassification(siteId, classificationId, name, schemaJson, schema, "joe");
  }

  private static SchemaAttributesRequired createDocTypeRequired() {
    return new SchemaAttributesRequired().attributeKey("docType")
        .allowedValues(List.of("invoice", "receipt"));
  }

  private static SchemaAttributesRequired createCategoryRequired(final List<String> allowedValues) {
    return new SchemaAttributesRequired().attributeKey("category").allowedValues(allowedValues);
  }

  private static void addAttribute(final String siteId, final String attributeKey) {
    attributeService.addAttribute(siteId, attributeKey, AttributeDataType.STRING,
        AttributeType.STANDARD);
  }

  /**
   * Set Sites Schema.
   */
  @Test
  public void testSetSitesSchema01() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      addAttribute(siteId, "category");
      addAttribute(siteId, "docType");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("A", "B"));
      SchemaAttributesRequired require1 = createDocTypeRequired();

      SchemaAttributesCompositeKey compositeKey =
          new SchemaAttributesCompositeKey().attributeKeys(List.of("category", "docType"));
      SchemaAttributes schemaAttributes = new SchemaAttributes()
          .required(List.of(require0, require1)).compositeKeys(List.of(compositeKey));

      // when
      Collection<ValidationError> errors = setSitesSchema(siteId, schemaAttributes);

      // then
      assertEquals(0, errors.size());
      Schema sitesSchema = service.getSitesSchema(siteId);
      assertNotNull(sitesSchema);

      SchemaCompositeKeyRecord compositeKeyRecord =
          service.getCompositeKey(siteId, List.of("docType", "category"));
      assertNotNull(compositeKeyRecord);
      assertNull(service.getCompositeKey(siteId, List.of("docType", "category123")));

      List<String> allowedValues = service.getSitesSchemaAttributeAllowedValues(siteId, "category");
      assertEquals(2, allowedValues.size());
      assertEquals("A,B", String.join(",", allowedValues));

      // given
      require0.allowedValues(List.of("1", "2", "3"));

      // when
      errors = setSitesSchema(siteId, schemaAttributes);

      // then
      assertEquals(0, errors.size());
      allowedValues = service.getSitesSchemaAttributeAllowedValues(siteId, "category");

      final int expected = 3;
      assertEquals(expected, allowedValues.size());
      assertEquals("1,2,3", String.join(",", allowedValues));

      allowedValues = service.getAttributeAllowedValues(siteId, "category");
      assertEquals(expected, allowedValues.size());
      assertEquals("1,2,3", String.join(",", allowedValues));
    }
  }

  /**
   * Set Classification.
   */
  @Test
  public void testSetClassification01() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      addAttribute(siteId, "category");
      addAttribute(siteId, "docType");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("Z", "Y"));
      SchemaAttributesRequired require1 = createDocTypeRequired();

      SchemaAttributesCompositeKey compositeKey =
          new SchemaAttributesCompositeKey().attributeKeys(List.of("category", "docType"));
      SchemaAttributes schemaAttributes = new SchemaAttributes()
          .required(List.of(require0, require1)).compositeKeys(List.of(compositeKey));

      // when
      ClassificationRecord classification =
          setClassification(siteId, null, "doc", schemaAttributes);

      // then
      final String classificationId = classification.getDocumentId();
      Schema sitesSchema = service.getSchema(classification);
      assertNotNull(sitesSchema);

      SchemaCompositeKeyRecord compositeKeyRecord =
          service.getCompositeKey(siteId, List.of("docType", "category"));
      assertNotNull(compositeKeyRecord);
      assertNull(service.getCompositeKey(siteId, List.of("docType", "category123")));

      List<String> allowedValues =
          service.getClassificationAttributeAllowedValues(siteId, classificationId, "category");
      assertEquals(2, allowedValues.size());
      assertEquals("Y,Z", String.join(",", allowedValues));

      // given
      require0.allowedValues(List.of("1", "2", "3"));

      // when
      setClassification(siteId, classificationId, "doc1", schemaAttributes);

      // then
      allowedValues =
          service.getClassificationAttributeAllowedValues(siteId, classificationId, "category");

      final int expected = 3;
      assertEquals(expected, allowedValues.size());
      assertEquals("1,2,3", String.join(",", allowedValues));

      allowedValues = service.getAttributeAllowedValues(siteId, "category");
      assertEquals(expected, allowedValues.size());
      assertEquals("1,2,3", String.join(",", allowedValues));
    }
  }

  /**
   * Get Allowed values across site schema and classification.
   */
  @Test
  void testSetClassification02() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      addAttribute(siteId, "category");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("Z", "Y"));
      setSitesSchema(siteId, new SchemaAttributes().required(List.of(require0)));

      SchemaAttributesRequired require1 = createCategoryRequired(List.of("A", "Z"));
      ClassificationRecord classification = setClassification(siteId, null, "doc1",
          new SchemaAttributes().required(List.of(require1)));

      // when
      List<String> allowedValues = service.getClassificationAttributeAllowedValues(siteId,
          classification.getDocumentId(), "category");

      // then
      final int expected = 3;
      assertEquals(expected, allowedValues.size());
      assertEquals("A,Y,Z", String.join(",", allowedValues));
    }
  }

  /**
   * Get Allowed values across site schema and multiple classifications.
   */
  @Test
  void testGetAttributeAllowedValues01() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      addAttribute(siteId, "category");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("Z", "Y"));
      setSitesSchema(siteId, new SchemaAttributes().required(List.of(require0)));

      SchemaAttributesRequired require1 = createCategoryRequired(List.of("A", "Z"));
      setClassification(siteId, null, "doc1", new SchemaAttributes().required(List.of(require1)));

      SchemaAttributesRequired require2 = createCategoryRequired(List.of("AA", "BB", "CC", "Z"));
      setClassification(siteId, null, "doc2", new SchemaAttributes().required(List.of(require2)));

      // when
      List<String> allowedValues = service.getAttributeAllowedValues(siteId, "category");

      // then
      final int expected = 6;
      assertEquals(expected, allowedValues.size());
      assertEquals("A,AA,BB,CC,Y,Z", String.join(",", allowedValues));
    }
  }
}