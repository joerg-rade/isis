package org.apache.isis.schema.utils;

import org.jmock.auto.Mock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.applib.services.bookmark.BookmarkService;
import org.apache.isis.applib.value.Blob;
import org.apache.isis.applib.value.Clob;
import org.apache.isis.core.unittestsupport.jmocking.JUnitRuleMockery2;
import org.apache.isis.schema.common.v1.BlobDto;
import org.apache.isis.schema.common.v1.ClobDto;
import org.apache.isis.schema.common.v1.ValueDto;
import org.apache.isis.schema.common.v1.ValueType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

public class CommonDtoUtils_setValueOn_Test {

    @Rule
    public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

    @Mock
    private BookmarkService mockBookmarkService;

    ValueDto valueDto;
    @Before
    public void setUp() throws Exception {
        valueDto = new ValueDto();
    }

    @Test
    public void when_blob_is_null() {
        CommonDtoUtils.setValueOn(valueDto, ValueType.BLOB, null, mockBookmarkService);
        final BlobDto blobDto = valueDto.getBlob();
        Assert.assertThat(blobDto, is(nullValue()));
    }

    @Test
    public void when_blob_is_not_null() {
        final Blob val = new Blob("image.png", "image/png", new byte[]{1,2,3,4,5});
        CommonDtoUtils.setValueOn(valueDto, ValueType.BLOB, val, mockBookmarkService);
        final BlobDto blobDto = valueDto.getBlob();
        Assert.assertThat(blobDto, is(notNullValue()));
        Assert.assertThat(blobDto.getBytes(), is(val.getBytes()));
        Assert.assertThat(blobDto.getName(), is(val.getName()));
        Assert.assertThat(blobDto.getMimeType(), is(val.getMimeType().toString()));
    }

    @Test
    public void when_clob_is_null() {
        CommonDtoUtils.setValueOn(valueDto, ValueType.CLOB, null, mockBookmarkService);
        final ClobDto clobDto = valueDto.getClob();
        Assert.assertThat(clobDto, is(nullValue()));
    }

    @Test
    public void when_clob_is_not_null() {
        final Clob val = new Clob("image.png", "image/png", new char[]{1,2,3,4,5});
        CommonDtoUtils.setValueOn(valueDto, ValueType.CLOB, val, mockBookmarkService);
        final ClobDto clobDto = valueDto.getClob();
        Assert.assertThat(clobDto, is(notNullValue()));
        Assert.assertThat(clobDto.getChars(), is(val.getChars()));
        Assert.assertThat(clobDto.getName(), is(val.getName()));
        Assert.assertThat(clobDto.getMimeType(), is(val.getMimeType().toString()));
    }
}