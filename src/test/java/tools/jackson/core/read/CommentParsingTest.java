package tools.jackson.core.read;

import java.io.*;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;

/**
 * Unit tests for verifying that support for (non-standard) comments
 * works as expected.
 */
public class CommentParsingTest
    extends tools.jackson.core.BaseTest
{
    final static String DOC_WITH_SLASHSTAR_COMMENT =
        "[ /* comment:\n ends here */ 1 /* one more ok to have \"unquoted\" and non-ascii: \u3456 \u00A0  */ ]"
        ;

    final static String DOC_WITH_SLASHSLASH_COMMENT =
        "[ // comment...\n 1 \r  // one more, not array: [] \u00A0 & \u3456  \n ]"
        ;

    /*
    /**********************************************************
    /* Test method wrappers
    /**********************************************************
     */

    /**
     * Unit test for verifying that by default comments are not
     * recognized.
     */
    public void testDefaultSettings()
    {
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        JsonParser p = f.createParser(ObjectReadContext.empty(), new StringReader("[ 1 ]"));
        p.close();
    }

    public void testCommentsDisabled()
    {
        _testDisabled(DOC_WITH_SLASHSTAR_COMMENT, MODE_INPUT_STREAM);
        _testDisabled(DOC_WITH_SLASHSLASH_COMMENT, MODE_INPUT_STREAM);
        _testDisabled(DOC_WITH_SLASHSTAR_COMMENT, MODE_INPUT_STREAM_THROTTLED);
        _testDisabled(DOC_WITH_SLASHSLASH_COMMENT, MODE_INPUT_STREAM_THROTTLED);
        _testDisabled(DOC_WITH_SLASHSTAR_COMMENT, MODE_READER);
        _testDisabled(DOC_WITH_SLASHSLASH_COMMENT, MODE_READER);
        _testDisabled(DOC_WITH_SLASHSTAR_COMMENT, MODE_DATA_INPUT);
        _testDisabled(DOC_WITH_SLASHSLASH_COMMENT, MODE_DATA_INPUT);
    }

    public void testCommentsEnabled()
    {
        _testEnabled(DOC_WITH_SLASHSTAR_COMMENT, MODE_INPUT_STREAM);
        _testEnabled(DOC_WITH_SLASHSLASH_COMMENT, MODE_INPUT_STREAM);
        _testEnabled(DOC_WITH_SLASHSTAR_COMMENT, MODE_INPUT_STREAM_THROTTLED);
        _testEnabled(DOC_WITH_SLASHSLASH_COMMENT, MODE_INPUT_STREAM_THROTTLED);
        _testEnabled(DOC_WITH_SLASHSTAR_COMMENT, MODE_READER);
        _testEnabled(DOC_WITH_SLASHSLASH_COMMENT, MODE_READER);
        _testEnabled(DOC_WITH_SLASHSTAR_COMMENT, MODE_DATA_INPUT);
        _testEnabled(DOC_WITH_SLASHSLASH_COMMENT, MODE_DATA_INPUT);
    }

    public void testCommentsWithUTF8()
    {
        final String JSON = "/* \u00a9 2099 Yoyodyne Inc. */\n [ \"bar? \u00a9\" ]\n";
        _testWithUTF8Chars(JSON, MODE_INPUT_STREAM);
        _testWithUTF8Chars(JSON, MODE_INPUT_STREAM_THROTTLED);
        _testWithUTF8Chars(JSON, MODE_READER);
        _testWithUTF8Chars(JSON, MODE_DATA_INPUT);
    }

    public void testYAMLCommentsBytes() {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                .build();
        _testYAMLComments(f, MODE_INPUT_STREAM);
        _testCommentsBeforePropValue(f, MODE_INPUT_STREAM, "# foo\n");
        _testYAMLComments(f, MODE_INPUT_STREAM_THROTTLED);
        _testCommentsBeforePropValue(f, MODE_INPUT_STREAM_THROTTLED, "# foo\n");
        _testYAMLComments(f, MODE_DATA_INPUT);
        _testCommentsBeforePropValue(f, MODE_DATA_INPUT, "# foo\n");
    }

    public void testYAMLCommentsChars() {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                .build();
        _testYAMLComments(f, MODE_READER);
        final String COMMENT = "# foo\n";
        _testCommentsBeforePropValue(f, MODE_READER, COMMENT);
        _testCommentsBetweenArrayValues(f, MODE_READER, COMMENT);
    }

    public void testCCommentsBytes() {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build();
        final String COMMENT = "/* foo */\n";
        _testCommentsBeforePropValue(f, MODE_INPUT_STREAM, COMMENT);
        _testCommentsBeforePropValue(f, MODE_INPUT_STREAM_THROTTLED, COMMENT);
        _testCommentsBeforePropValue(f, MODE_DATA_INPUT, COMMENT);
    }

    public void testCCommentsChars() {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build();
        final String COMMENT = "/* foo */\n";
        _testCommentsBeforePropValue(f, MODE_READER, COMMENT);
    }

    public void testCppCommentsBytes() {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build();
        final String COMMENT = "// foo\n";
        _testCommentsBeforePropValue(f, MODE_INPUT_STREAM, COMMENT);
        _testCommentsBeforePropValue(f, MODE_INPUT_STREAM_THROTTLED, COMMENT);
        _testCommentsBeforePropValue(f, MODE_DATA_INPUT, COMMENT);
    }

    public void testCppCommentsChars() {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build();
        final String COMMENT = "// foo \n";
        _testCommentsBeforePropValue(f, MODE_READER, COMMENT);
    }

    @SuppressWarnings("resource")
    private void _testCommentsBeforePropValue(JsonFactory f,
            int mode, String comment)
    {
        for (String arg : new String[] {
                ":%s123",
                " :%s123",
                "\t:%s123",
                ": %s123",
                ":\t%s123",
        }) {
            String commented = String.format(arg, comment);

            final String DOC = "{\"abc\"" + commented + "}";
            JsonParser p = createParser(f, mode, DOC);
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            JsonToken t = null;
            try {
                t = p.nextToken();
            } catch (Exception e) {
                throw new RuntimeException("Failed on '"+DOC+"' due to "+e, e);
            }
            assertEquals(JsonToken.PROPERTY_NAME, t);

            try {
                t = p.nextToken();
            } catch (Exception e) {
                throw new RuntimeException("Failed on '"+DOC+"' due to "+e, e);
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(123, p.getIntValue());
            assertEquals(JsonToken.END_OBJECT, p.nextToken());
            p.close();
        }

    }

    @SuppressWarnings("resource")
    private void _testCommentsBetweenArrayValues(JsonFactory f,
            int mode, String comment)
    {
        for (String tmpl : new String[] {
                "%s,",
                " %s,",
                "\t%s,",
                "%s ,",
                "%s\t,",
                " %s ,",
                "\t%s\t,",
                "\n%s,",
                "%s\n,",
        }) {
            String commented = String.format(tmpl, comment);

            final String DOC = "[1"+commented+"2]";
            JsonParser p = createParser(f, mode, DOC);
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = null;
            try {
                t = p.nextToken();
            } catch (Exception e) {
                throw new RuntimeException("Failed on '"+DOC+"' due to "+e, e);
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(1, p.getIntValue());

            try {
                t = p.nextToken();
            } catch (Exception e) {
                throw new RuntimeException("Failed on '"+DOC+"' due to "+e, e);
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(2, p.getIntValue());
            assertEquals(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }

    }

    private void _testYAMLComments(JsonFactory f, int mode)
    {
        final String DOC = "# foo\n"
                +" {\"a\" # xyz\n"
                +" : # foo\n"
                +" 1, # more\n"
                +"\"b\": [ \n"
                +" #all!\n"
                +" 3 #yay!\n"
                +"] # foobar\n"
                +"} # x"
                ;
        JsonParser p = createParser(f, mode, DOC);
        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("b", p.currentName());
        assertEquals(JsonToken.START_ARRAY, p.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testWithUTF8Chars(String doc, int mode)
    {
        // should basically just stream through
        JsonParser p = _createParser(doc, mode, true);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();
    }

    private void _testDisabled(String doc, int mode)
    {
        JsonParser p = _createParser(doc, mode, false);
        try {
            p.nextToken();
            fail("Expected exception for unrecognized comment");
        } catch (StreamReadException je) {
            // Should have something denoting that user may want to enable 'ALLOW_COMMENTS'
            verifyException(je, "ALLOW_COMMENTS");
        }
        p.close();
    }

    private void _testEnabled(String doc, int mode)
    {
        JsonParser p = _createParser(doc, mode, true);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private JsonParser _createParser(String doc, int mode, boolean enabled)
    {
        final JsonFactory f = JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, enabled)
                .build();
        JsonParser p = createParser(f, mode, doc);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        return p;
    }
}
