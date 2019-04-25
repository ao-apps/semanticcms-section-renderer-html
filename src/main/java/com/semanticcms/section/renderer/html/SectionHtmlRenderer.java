/*
 * semanticcms-section-renderer-html - Sections rendered as HTML in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2019  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-section-renderer-html.
 *
 * semanticcms-section-renderer-html is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-section-renderer-html is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-section-renderer-html.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.section.renderer.html;

import com.aoindustries.encoding.MediaWriter;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.textInXhtmlAttributeEncoder;
import static com.aoindustries.encoding.TextInXhtmlEncoder.encodeTextInXhtml;
import com.aoindustries.io.buffer.BufferResult;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.renderer.html.PageIndex;
import com.semanticcms.section.model.Section;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.jsp.SkipPageException;

final public class SectionHtmlRenderer {

	public static void writeSection(
		Writer out,
		ElementContext context,
		Section section,
		PageIndex pageIndex
	) throws IOException, ServletException, SkipPageException {
		// If this is the first section in the page, write the table of contents
		Page page = section.getPage();
		if(page != null) {
			List<Section> topLevelSections = page.findTopLevelElements(Section.class);
			if(!topLevelSections.isEmpty() && topLevelSections.get(0) == section) {
				try {
					context.include(
						"/semanticcms-section-renderer-html/toc.inc.jspx",
						out,
						Collections.singletonMap("page", page)
					);
				} catch(IOException | ServletException | SkipPageException | RuntimeException e) {
					throw e;
				} catch(Exception e) {
					throw new ServletException(e);
				}
			}
		}
		// Count the section level by finding all sections in the parent elements
		int sectionLevel = 2; // <h1> is reserved for page titles
		com.semanticcms.core.model.Element parentElement = section.getParentElement();
		while(parentElement != null) {
			if(parentElement instanceof Section) sectionLevel++;
			parentElement = parentElement.getParentElement();
		}
		// Highest tag is <h6>
		if(sectionLevel > 6) throw new IOException("Sections exceeded depth of h6 (including page as h1): sectionLevel = " + sectionLevel);

		out.write("<section><h");
		char sectionLevelChar = (char)('0' + sectionLevel);
		out.write(sectionLevelChar);
		String id = section.getId();
		if(id != null) {
			out.write(" id=\"");
			PageIndex.appendIdInPage(
				pageIndex,
				section.getPage(),
				id,
				new MediaWriter(textInXhtmlAttributeEncoder, out)
			);
			out.write('"');
		}
		out.write('>');
		encodeTextInXhtml(section.getLabel(), out);
		out.write("</h");
		out.write(sectionLevelChar);
		out.write('>');
		BufferResult body = section.getBody();
		if(body.getLength() > 0) {
			out.write("<div class=\"h");
			out.write(sectionLevelChar);
			out.write("Content\">");
			body.writeTo(new NodeBodyWriter(section, out, context));
			out.write("</div>");
		}
		out.write("</section>");
	}

	/**
	 * Make no instances.
	 */
	private SectionHtmlRenderer() {
	}
}
