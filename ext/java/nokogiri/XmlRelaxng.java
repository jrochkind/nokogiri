/**
 * (The MIT License)
 *
 * Copyright (c) 2008 - 2010:
 *
 * * {Aaron Patterson}[http://tenderlovemaking.com]
 * * {Mike Dalessio}[http://mike.daless.io]
 * * {Charles Nutter}[http://blog.headius.com]
 * * {Sergio Arbeo}[http://www.serabe.com]
 * * {Patrick Mahoney}[http://polycrystal.org]
 * * {Yoko Harada}[http://yokolet.blogspot.com]
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * 'Software'), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nokogiri;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import nokogiri.internals.SchemaErrorHandler;

import org.iso_relax.verifier.Schema;
import org.iso_relax.verifier.Verifier;
import org.iso_relax.verifier.VerifierConfigurationException;
import org.iso_relax.verifier.VerifierFactory;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * Class for Nokogiri::XML::RelaxNG
 * 
 * @author sergio
 */
@JRubyClass(name="Nokogiri::XML::RelaxNG", parent="Nokogiri::XML::Schema")
public class XmlRelaxng extends XmlSchema{

    public XmlRelaxng(Ruby ruby, RubyClass klazz) {
        super(ruby, klazz);
    }

    private Schema getSchema(ThreadContext context) {
        InputStream is = null;
        VerifierFactory factory = new com.thaiopensource.relaxng.jarv.VerifierFactoryImpl();
        if(this.source instanceof StreamSource) {
            StreamSource ss = (StreamSource) this.source;
            is = ss.getInputStream();
        } else /*if (this.source instanceof DOMSource)*/{
            DOMSource ds = (DOMSource) this.source;
            StringWriter xmlAsWriter = new StringWriter();
            StreamResult result = new StreamResult(xmlAsWriter);
            try {
                TransformerFactory.newInstance().newTransformer().transform(ds, result);
            } catch (TransformerConfigurationException ex) {
                throw context.getRuntime()
                    .newRuntimeError("Could not parse document: "+ex.getMessage());
            } catch (TransformerException ex) {
                throw context.getRuntime()
                    .newRuntimeError("Could not parse document: "+ex.getMessage());
            }
            try {
                is = new ByteArrayInputStream(xmlAsWriter.toString().getBytes("UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                throw context.getRuntime()
                    .newRuntimeError("Could not parse document: "+ex.getMessage());
            }
        }

        try {
            return factory.compileSchema(is);
        } catch (VerifierConfigurationException ex) {
            throw context.getRuntime()
                .newRuntimeError("Could not parse document: "+ex.getMessage());
        } catch (SAXException ex) {
            throw context.getRuntime()
                .newRuntimeError("Could not parse document: "+ex.getMessage());
        } catch (IOException ex) {
            throw context.getRuntime().newIOError(ex.getMessage());
        }
    }

//
//    protected static XmlSchema createSchemaFromSource(ThreadContext context,
//            IRubyObject klazz, Source source) {
//
//        Ruby ruby = context.getRuntime();
//
//        XmlSchema schema = new XmlSchema(ruby, (RubyClass) klazz);
//
//        try {
//            schema.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
//                    .newSchema(source);
//        } catch(SAXException ex) {
//            throw ruby.newRuntimeError("Could not parse document: "+ex.getMessage());
//        }
//
//        schema.setInstanceVariable("@errors", ruby.newEmptyArray());
//
//        return schema;
//    }
//
//    @JRubyMethod(meta=true)
//    public static IRubyObject from_document(ThreadContext context,
//            IRubyObject klazz, IRubyObject document) {
//        XmlDocument doc = ((XmlDocument) ((XmlNode) document).document(context));
//
//        RubyArray errors = (RubyArray) doc.getInstanceVariable("@errors");
//
//        if(!errors.isEmpty()) {
//            throw new RaiseException((XmlSyntaxError) errors.first());
//        }
//
//        DOMSource source = new DOMSource(doc.getDocument());
//
//        IRubyObject uri = doc.url(context);
//
//        if(!uri.isNil()) {
//            source.setSystemId(uri.convertToString().asJavaString());
//        }
//
//        return createSchemaFromSource(context, klazz, source);
//    }
//
//    @JRubyMethod(meta=true)
//    public static IRubyObject read_memory(ThreadContext context,
//            IRubyObject klazz, IRubyObject content) {
//
//        String data = content.convertToString().asJavaString();
//
//        return createSchemaFromSource(context, klazz,
//                new StreamSource(new StringReader(data)));
//    }
//
    @Override
    @JRubyMethod(visibility=Visibility.PRIVATE)
    public IRubyObject validate_document(ThreadContext context, IRubyObject document) {
        Ruby ruby = context.getRuntime();

        Document doc = ((XmlDocument) document).getDocument();

        Schema schema = this.getSchema(context);

        Verifier verifier;
        try {
            verifier = schema.newVerifier();
        } catch (VerifierConfigurationException ex) {
            throw context.getRuntime()
                .newRuntimeError("Could not parse document: "+ex.getMessage());
        }
        
        RubyArray errors = (RubyArray) this.getInstanceVariable("@errors");
        ErrorHandler errorHandler = new SchemaErrorHandler(ruby, errors);

        verifier.setErrorHandler(errorHandler);
        try {
            verifier.verify(doc);
        } catch (SAXException ex) {
            errors.append(new XmlSyntaxError(ruby, ex));
        }

        return errors;
    }
}
