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
package com.formkiq.stacks.dynamodb;

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCS;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.AttributeValueToMap;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.DynamodbVersionRecord;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * 
 * {@link DocumentVersionService} implementation.
 *
 */
@Reflectable
public class DocumentVersionServiceDynamoDb implements DocumentVersionService {

  /** The Default maximum results returned. */
  private static final int MAX_RESULTS = 100;
  /** {@link SimpleDateFormat} in ISO Standard format. */
  private final SimpleDateFormat df = DateUtil.getIsoDateFormatter();
  /** DynamoDB Document Versions Table Name. */
  private String tableName = null;

  @Override
  public void deleteAllVersionIds(final DynamoDbClient client, final String siteId,
      final String documentId) {

    Map<String, AttributeValue> startkey = null;
    String pk = createDatabaseKey(siteId, PREFIX_DOCS + documentId);

    String documentVersionsTable = getDocumentVersionsTableName();
    DynamoDbService db = new DynamoDbServiceImpl(client, documentVersionsTable);

    do {

      QueryResponse response =
          db.query(new QueryConfig(), AttributeValue.fromS(pk), startkey, MAX_RESULTS);

      List<Map<String, AttributeValue>> results = response.items();

      for (Map<String, AttributeValue> map : results) {
        db.deleteItem(map.get(PK), map.get(SK));
      }

      startkey = response.lastEvaluatedKey();

    } while (startkey != null && !startkey.isEmpty());
  }

  @Override
  public String getDocumentVersionsTableName() {
    return this.tableName;
  }

  @Override
  public String getVersionId(final Map<String, AttributeValue> attrs) {
    return attrs.containsKey(S3VERSION_ATTRIBUTE) ? attrs.get(S3VERSION_ATTRIBUTE).s() : null;
  }

  @Override
  public Map<String, AttributeValue> get(final DynamoDbConnectionBuilder connection,
      final String siteId, final String documentId, final String versionKey) {

    Map<String, AttributeValue> attrs = Collections.emptyMap();
    String documentVersionsTable = getDocumentVersionsTableName();

    if (!isEmpty(documentVersionsTable) && !isEmpty(versionKey)) {

      String pk = createDatabaseKey(siteId, PREFIX_DOCS + documentId);

      DynamoDbService db = new DynamoDbServiceImpl(connection, documentVersionsTable);
      attrs = db.get(AttributeValue.fromS(pk), AttributeValue.fromS(versionKey));
    }

    return attrs;
  }

  @Override
  public void initialize(final Map<String, String> map) {
    this.tableName = map.get("DOCUMENT_VERSIONS_TABLE");
  }

  @Override
  public List<Map<String, AttributeValue>> addRecords(final DynamoDbClient client,
      final String siteId, final Collection<? extends DynamodbVersionRecord<?>> records) {

    List<Map<String, AttributeValue>> attrs =
        records.stream().map(r -> r.getVersionedAttributes(siteId)).toList();

    DynamoDbService db = new DynamoDbServiceImpl(client, getDocumentVersionsTableName());
    db.putItems(attrs);

    return attrs;
  }

  private String getSk(final Map<String, AttributeValue> previous, final String version) {
    return previous.get(SK).s() + TAG_DELIMINATOR + this.df.format(new Date()) + TAG_DELIMINATOR
        + "v" + version;
  }

  @Override
  public DocumentItem getDocumentItem(final DocumentService documentService, final String siteId,
      final String documentId, final String versionKey,
      final Map<String, AttributeValue> versionAttributes) {

    DocumentItem item;

    if (!Strings.isEmpty(versionKey)) {
      item = new DynamicDocumentItem(new AttributeValueToMap().apply(versionAttributes));
      item.setDocumentId(documentId);
    } else {
      item = documentService.findDocument(siteId, documentId);
    }

    return item;
  }
}
