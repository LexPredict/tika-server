/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2020 ContraxSuite, LLC
 */

package com.lexpredict.tika;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


class XFAExtractor {

    private static final Pattern XFA_TEMPLATE_ANY_VERSION = Pattern.compile("^http://www.xfa.org/schema/xfa-template");
    private static final Pattern TEXT_PATTERN =
            Pattern.compile("^(speak|text|contents-richtext|toolTip|exData)$");

    private static final String XFA_DATA_NS = "http://www.xfa.org/schema/xfa-data/1.0/";

    private static final String FIELD_LN = "field";
    private static final QName XFA_DATA = new QName(XFA_DATA_NS, "data");

    private final Matcher xfaTemplateMatcher;//namespace any version
    private final Matcher textMatcher;

    XFAExtractor() {
        xfaTemplateMatcher = XFA_TEMPLATE_ANY_VERSION.matcher("");
        textMatcher = TEXT_PATTERN.matcher("");
    }

    void extract(InputStream xfaIs, XHTMLContentHandler xhtml, Metadata m, ParseContext context)
            throws XMLStreamException, SAXException {
        xhtml.startElement("div", "class", "xfa_content");

        Map<String, String> pdfObjRToValues = new HashMap<>();

        //for now, store and dump the fields in insertion order
        Map<String, XFAExtractor.XFAField> namedFields = new LinkedHashMap<>();

        //The strategy is to cache the fields in fields
        //and cache the values in pdfObjRToValues while
        //handling the text etc along the way.
        //
        //As a final step, dump the merged fields and the values.

        XMLStreamReader reader = context.getXMLInputFactory().createXMLStreamReader(xfaIs);
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT :
                    QName name = reader.getName();
                    String localName = name.getLocalPart();
                    if (xfaTemplateMatcher.reset(name.getNamespaceURI()).find() &&
                            FIELD_LN.equals(name.getLocalPart())) {
                        handleField(reader, namedFields);
                    } else if (XFA_DATA.equals(name)) {//full qname match is important!
                        loadData(reader, pdfObjRToValues);
                    } else if (textMatcher.reset(localName).find()) {
                        scrapeTextUntil(reader, xhtml, name);
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT :
                    break;
            }
        }

        if (namedFields.size() == 0) {
            xhtml.endElement("xfa_content");
            return;
        }
        //now dump fields and values
        xhtml.startElement("div", "class", "xfa_form");
        xhtml.startElement("ol");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, XFAExtractor.XFAField> e : namedFields.entrySet()) {
            String fieldName = e.getKey();
            XFAExtractor.XFAField field = e.getValue();
            String fieldValue = pdfObjRToValues.get(fieldName);
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "fieldName", "fieldName", "CDATA", fieldName);

            String displayFieldName = (field.toolTip == null ||
                    field.toolTip.trim().length() == 0) ? fieldName : field.toolTip;

            sb.append(displayFieldName).append(": ");
            if (fieldValue != null) {
                sb.append(fieldValue);
            }

            xhtml.startElement("li", attrs);
            xhtml.characters(sb.toString());
            xhtml.endElement("li");
            sb.setLength(0);
        }
        xhtml.endElement("ol");
        xhtml.endElement("div");
        xhtml.endElement("xfa_content");
    }

    //try to scrape the text until the endElement
    private void scrapeTextUntil(XMLStreamReader reader, XHTMLContentHandler xhtml,
                                 QName endElement) throws XMLStreamException, SAXException {
        StringBuilder buffer = new StringBuilder();
        boolean keepGoing = true;
        while (reader.hasNext() && keepGoing) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    break;
                case XMLStreamConstants.CHARACTERS:
                    int start = reader.getTextStart();
                    int length = reader.getTextLength();
                    buffer.append(reader.getTextCharacters(),
                            start,
                            length);
                    break;

                case XMLStreamConstants.CDATA:
                    start = reader.getTextStart();
                    length = reader.getTextLength();
                    buffer.append(reader.getTextCharacters(),
                            start,
                            length);
                    break;

                case (XMLStreamConstants.END_ELEMENT):
                    if (reader.getName().equals(endElement)) {
                        keepGoing = false;
                    } else if ("p".equals(reader.getName().getLocalPart())) {
                        xhtml.element("p", buffer.toString());
                        buffer.setLength(0);
                    }
                    break;
            }
        }
        String remainder = buffer.toString();
        if (remainder.trim().length() > 0) {
            xhtml.element("p", remainder);
        }
    }


    private String scrapeTextUntil(XMLStreamReader reader, QName endElement) throws XMLStreamException {
        StringBuilder buffer = new StringBuilder();
        boolean keepGoing = true;
        while (reader.hasNext() && keepGoing) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    break;
                case XMLStreamConstants.CHARACTERS:
                    int start = reader.getTextStart();
                    int length = reader.getTextLength();
                    buffer.append(reader.getTextCharacters(),
                            start,
                            length);
                    break;

                case XMLStreamConstants.CDATA:
                    start = reader.getTextStart();
                    length = reader.getTextLength();
                    buffer.append(reader.getTextCharacters(),
                            start,
                            length);
                    break;

                case (XMLStreamConstants.END_ELEMENT):
                    if (reader.getName().equals(endElement)) {
                        keepGoing = false;
                    } else if ("p".equals(reader.getName().getLocalPart())) {
                        buffer.append("\n");
                    }
                    break;
            }
        }
        return buffer.toString();
    }

    private void loadData(XMLStreamReader reader, Map<String, String> pdfObjRToValues)
            throws XMLStreamException {
        //reader is at the "xfa:data" element
        //scrape the contents from the text containing nodes
        StringBuilder buffer = new StringBuilder();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case (XMLStreamConstants.START_ELEMENT) :
                    break;
                case XMLStreamConstants.CHARACTERS:
                    int start = reader.getTextStart();
                    int length = reader.getTextLength();
                    buffer.append(reader.getTextCharacters(),
                            start,
                            length);
                    break;

                case XMLStreamConstants.CDATA:
                    start = reader.getTextStart();
                    length = reader.getTextLength();
                    buffer.append(reader.getTextCharacters(),
                            start,
                            length);
                    break;

                case (XMLStreamConstants.END_ELEMENT) :
                    if (buffer.length() > 0) {
                        String localName = reader.getLocalName();
                        pdfObjRToValues.put(localName, buffer.toString());
                        buffer.setLength(0);
                    }
                    if (XFA_DATA.equals(reader.getName())) {
                        return;
                    }
                    break;

            }
        }
    }

    private void handleField(XMLStreamReader reader, Map<String, XFAExtractor.XFAField> fields) throws XMLStreamException {
        //reader is set to the field element
        String fieldName = findFirstAttributeValue(reader, "name");
        String pdfObjRef = "";
        String toolTip = "";
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT :
                    if ("toolTip".equals(reader.getName().getLocalPart())) {
                        toolTip = scrapeTextUntil(reader, reader.getName());
                    }
                    // add checkbutton, etcif (reader.getName().equals())
                    break;
                case XMLStreamConstants.END_ELEMENT :
                    if (xfaTemplateMatcher.reset(reader.getName().getNamespaceURI()).find() &&
                            FIELD_LN.equals(reader.getName().getLocalPart())) {
                        if (fieldName != null) {
                            fields.put(fieldName, new XFAExtractor.XFAField(fieldName, toolTip, pdfObjRef));
                        }
                        return;
                    }
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    if ("PDF_OBJR".equals(reader.getPITarget())) {
                        pdfObjRef = reader.getPIData();
                    }
                    break;

            }
        }
    }

    private String findFirstAttributeValue(XMLStreamReader reader, String name) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String n = reader.getAttributeLocalName(i);
            if (name.equals(n)) {
                return reader.getAttributeValue(i);
            }
        }
        return "";
    }

    class XFAField {
        String fieldName;
        String toolTip;
        String pdfObjRef;
        String value;

        public XFAField(String fieldName, String toolTip, String pdfObjRef) {
            this.fieldName = fieldName;
            this.toolTip = toolTip;
            this.pdfObjRef = pdfObjRef;
        }

        @Override
        public String toString() {
            return "XFAField{" +
                    "fieldName='" + fieldName + '\'' +
                    ", toolTip='" + toolTip + '\'' +
                    ", pdfObjRef='" + pdfObjRef + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}

