package net.gsantner.opoc.util;

import static net.gsantner.opoc.util.FileUtils.getFileName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.TimeZone;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class FileUtilsTest {

    private static final String FILE_PATH = "/documents/txt/";
    private static final String FILE_NAME = "someText.pdf";

    @Mock
    FileUtils fileUtils;

    @Mock
    File file;

    @Before
    public void setUp() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void getFormattedCorrectSize() {
        when(file.length()).thenReturn(6427463L);
        assertThat(fileUtils.getFormattedSize(file), is("6,13 MB"));
    }

    @Test
    public void getFormattedCorrectDate() {
        when(file.lastModified()).thenReturn(0L);
        assertThat(fileUtils.getFormattedDate(file), is("Thu, Jan 01 at 00:00"));
    }

    @Test
    public void getFileDirectoryPath() {
        assertThat(fileUtils.getFileDirectoryPath(FILE_PATH + FILE_NAME), is(FILE_PATH));
        assertThat(fileUtils.getFileDirectoryPath(""), is(""));
    }

    @Test
    public void getCorrectFileName() {
        assertThat(getFileName(FILE_PATH + FILE_NAME), is(FILE_NAME));
        assertThat(getFileName(""), is(""));
    }

    @Test
    public void getFileNameWithoutExtension() {
        assertThat(fileUtils.getFileNameWithoutExtension(FILE_PATH + FILE_NAME), is("someText"));
        assertThat(fileUtils.getFileNameWithoutExtension(""), is(""));
    }
}
