/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.core.util;

import com.google.common.collect.Lists;
import java.util.Collections;
import org.apache.pinot.common.tier.TierFactory;
import org.apache.pinot.spi.config.table.IngestionConfig;
import org.apache.pinot.spi.config.table.TableConfig;
import org.apache.pinot.spi.config.table.TableType;
import org.apache.pinot.spi.config.table.TierConfig;
import org.apache.pinot.spi.config.table.ingestion.FilterConfig;
import org.apache.pinot.spi.config.table.ingestion.TransformConfig;
import org.apache.pinot.spi.utils.builder.TableConfigBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests for the validations in {@link TableConfigUtils}
 */
public class TableConfigUtilsTest {

  @Test
  public void validateIngestionConfig() {
    // null ingestion config
    TableConfig tableConfig =
        new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setIngestionConfig(null).build();
    TableConfigUtils.validate(tableConfig);

    // null filter config, transform config
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable")
        .setIngestionConfig(new IngestionConfig(null, null)).build();
    TableConfigUtils.validate(tableConfig);

    // null filter function
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable")
        .setIngestionConfig(new IngestionConfig(new FilterConfig(null), null)).build();
    TableConfigUtils.validate(tableConfig);

    // valid filterFunction
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable")
        .setIngestionConfig(new IngestionConfig(new FilterConfig("startsWith(columnX, \"myPrefix\")"), null)).build();
    TableConfigUtils.validate(tableConfig);

    // valid filterFunction
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable")
        .setIngestionConfig(new IngestionConfig(new FilterConfig("Groovy({x == 10}, x)"), null)).build();
    TableConfigUtils.validate(tableConfig);

    // invalid filter function
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable")
        .setIngestionConfig(new IngestionConfig(new FilterConfig("Groovy(badExpr)"), null)).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should fail on invalid filter function string");
    } catch (IllegalStateException e) {
      // expected
    }

    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable")
        .setIngestionConfig(new IngestionConfig(new FilterConfig("fakeFunction(xx)"), null)).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should fail for invalid filter function");
    } catch (IllegalStateException e) {
      // expected
    }

    // empty transform configs
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable")
        .setIngestionConfig(new IngestionConfig(null, Collections.emptyList())).build();
    TableConfigUtils.validate(tableConfig);

    // valid transform configs
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setIngestionConfig(
        new IngestionConfig(null, Lists.newArrayList(new TransformConfig("myCol", "reverse(anotherCol)")))).build();
    TableConfigUtils.validate(tableConfig);

    // valid transform configs
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setIngestionConfig(
        new IngestionConfig(null, Lists.newArrayList(new TransformConfig("myCol", "reverse(anotherCol)"),
            new TransformConfig("transformedCol", "Groovy({x+y}, x, y)")))).build();
    TableConfigUtils.validate(tableConfig);

    // null transform column name
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setIngestionConfig(
        new IngestionConfig(null, Lists.newArrayList(new TransformConfig(null, "reverse(anotherCol)")))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should fail for null column name in transform config");
    } catch (IllegalStateException e) {
      // expected
    }

    // null transform function string
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable")
        .setIngestionConfig(new IngestionConfig(null, Lists.newArrayList(new TransformConfig("myCol", null)))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should fail for null transform function in transform config");
    } catch (IllegalStateException e) {
      // expected
    }

    // invalid function
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setIngestionConfig(
        new IngestionConfig(null, Lists.newArrayList(new TransformConfig("myCol", "fakeFunction(col)")))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should fail for invalid transform function in transform config");
    } catch (IllegalStateException e) {
      // expected
    }

    // invalid function
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setIngestionConfig(
        new IngestionConfig(null, Lists.newArrayList(new TransformConfig("myCol", "Groovy(badExpr)")))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should fail for invalid transform function in transform config");
    } catch (IllegalStateException e) {
      // expected
    }

    // input field name used as destination field
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setIngestionConfig(
        new IngestionConfig(null, Lists.newArrayList(new TransformConfig("myCol", "reverse(myCol)")))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should fail due to use of myCol as arguments and columnName");
    } catch (IllegalStateException e) {
      // expected
    }

    // input field name used as destination field
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setIngestionConfig(
        new IngestionConfig(null,
            Lists.newArrayList(new TransformConfig("myCol", "Groovy({x + y + myCol}, x, myCol, y)")))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should fail due to use of myCol as arguments and columnName");
    } catch (IllegalStateException e) {
      // expected
    }

    // duplicate transform config
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setIngestionConfig(
        new IngestionConfig(null,
            Lists.newArrayList(new TransformConfig("myCol", "reverse(x)"), new TransformConfig("myCol", "lower(y)"))))
        .build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should fail due to duplicate transform config");
    } catch (IllegalStateException e) {
      // expected
    }

    // chained transform functions
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setIngestionConfig(
        new IngestionConfig(null,
            Lists.newArrayList(new TransformConfig("a", "reverse(x)"), new TransformConfig("b", "lower(a)")))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should fail due to using transformed column 'a' as argument for transform function of column 'b'");
    } catch (IllegalStateException e) {
      // expected
    }
  }

  @Test
  public void validateTierConfigs() {
    // null tier configs
    TableConfig tableConfig =
        new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(null).build();
    TableConfigUtils.validate(tableConfig);

    // empty tier configs
    tableConfig =
        new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Collections.emptyList())
            .build();
    TableConfigUtils.validate(tableConfig);

    // 1 tier configs
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Lists
        .newArrayList(new TierConfig("tier1", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "30d",
            TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier1_tag_OFFLINE"))).build();
    TableConfigUtils.validate(tableConfig);

    // 2 tier configs, case insensitive check
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Lists
        .newArrayList(new TierConfig("tier1", TierFactory.TIME_SEGMENT_SELECTOR_TYPE.toLowerCase(), "30d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier1_tag_OFFLINE"),
            new TierConfig("tier2", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "40d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE.toLowerCase(), "tier2_tag_OFFLINE"))).build();
    TableConfigUtils.validate(tableConfig);

    //realtime table
    tableConfig = new TableConfigBuilder(TableType.REALTIME).setTableName("testTable").setTimeColumnName("millis")
        .setTierConfigList(Lists.newArrayList(new TierConfig("tier1", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "30d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE.toLowerCase(), "tier1_tag_OFFLINE"),
            new TierConfig("tier2", TierFactory.TIME_SEGMENT_SELECTOR_TYPE.toLowerCase(), "40d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier2_tag_OFFLINE"))).build();
    TableConfigUtils.validate(tableConfig);

    // tier name empty
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Lists
        .newArrayList(
            new TierConfig("", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "30d", TierFactory.PINOT_SERVER_STORAGE_TYPE,
                "tier1_tag_OFFLINE"))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should have failed due to empty tier name");
    } catch (IllegalStateException e) {
      // expected
    }

    // tier name repeats
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Lists
        .newArrayList(new TierConfig("sameTierName", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "30d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier1_tag_OFFLINE"),
            new TierConfig("sameTierName", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "100d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier2_tag_OFFLINE"))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should have failed due to duplicate tier name");
    } catch (IllegalStateException e) {
      // expected
    }

    // segmentSelectorType invalid
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Lists
        .newArrayList(new TierConfig("tier1", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "30d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier1_tag_OFFLINE"),
            new TierConfig("tier2", "unsupportedSegmentSelector", "40d", TierFactory.PINOT_SERVER_STORAGE_TYPE,
                "tier2_tag_OFFLINE"))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should have failed due to invalid segmentSelectorType");
    } catch (IllegalStateException e) {
      // expected
    }

    // segmentAge not provided for TIME segmentSelectorType
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Lists
        .newArrayList(new TierConfig("tier1", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, null,
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier1_tag_OFFLINE"),
            new TierConfig("tier2", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "40d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier2_tag_OFFLINE"))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should have failed due to missing segmentAge");
    } catch (IllegalStateException e) {
      // expected
    }

    // segmentAge invalid
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Lists
        .newArrayList(new TierConfig("tier1", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "30d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier1_tag_OFFLINE"),
            new TierConfig("tier2", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "3600",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier2_tag_OFFLINE"))).build();

    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should have failed due to invalid segment age");
    } catch (IllegalStateException e) {
      // expected
    }

    // storageType invalid
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Lists
        .newArrayList(new TierConfig("tier1", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "30d", "unsupportedStorageType",
            "tier1_tag_OFFLINE"), new TierConfig("tier2", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "40d",
            TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier2_tag_OFFLINE"))).build();

    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should have failed due to invalid storage type");
    } catch (IllegalStateException e) {
      // expected
    }

    // serverTag not provided for PINOT_SERVER storageType
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Lists
        .newArrayList(new TierConfig("tier1", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "30d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier1_tag_OFFLINE"),
            new TierConfig("tier2", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "40d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, null))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should have failed due to ");
    } catch (IllegalStateException e) {
      // expected
    }

    // serverTag invalid
    tableConfig = new TableConfigBuilder(TableType.OFFLINE).setTableName("testTable").setTierConfigList(Lists
        .newArrayList(new TierConfig("tier1", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "30d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier1_tag"),
            new TierConfig("tier2", TierFactory.TIME_SEGMENT_SELECTOR_TYPE, "40d",
                TierFactory.PINOT_SERVER_STORAGE_TYPE, "tier2_tag_OFFLINE"))).build();
    try {
      TableConfigUtils.validate(tableConfig);
      Assert.fail("Should have failed due to invalid server tag");
    } catch (IllegalStateException e) {
      // expected
    }
  }
}
