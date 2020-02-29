/*
 * semanticcms-section-renderer-html - Sections rendered as HTML in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2019, 2020  AO Industries, Inc.
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
import com.aoindustries.html.Html;
import com.aoindustries.io.buffer.BufferResult;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.renderer.html.PageIndex;
import com.semanticcms.section.model.Aside;
import com.semanticcms.section.model.Nav;
import com.semanticcms.section.model.Section;
import com.semanticcms.section.model.SectioningContent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.jsp.SkipPageException;

// TODO: Implement with https://www.w3.org/TR/wai-aria-1.1/#aria-label
final public class SectionHtmlRenderer {

	public static void writeSectioningContent(
		Html html,
		ElementContext context,
		SectioningContent sectioningContent,
		String htmlElement,
		PageIndex pageIndex
	) throws IOException, ServletException, SkipPageException {
		// If this is the first sectioning content in the page, write the table of contents
		Page page = sectioningContent.getPage();
		if(page != null) {
			List<SectioningContent> topLevelSectioningContents = page.findTopLevelElements(SectioningContent.class);
			if(!topLevelSectioningContents.isEmpty() && topLevelSectioningContents.get(0) == sectioningContent) {
				try {
					context.include(
						"/semanticcms-section-renderer-html/toc.inc.jspx",
						html.out,
						Collections.singletonMap("page", page)
					);
				} catch(IOException | ServletException | SkipPageException | RuntimeException e) {
					throw e;
				} catch(Exception e) {
					throw new ServletException(e);
				}
			}
		}
		// Count the sectioning level by finding all sectioning contents in the parent elements
		int sectioningLevel = 2; // <h1> is reserved for page titles
		com.semanticcms.core.model.Element parentElement = sectioningContent.getParentElement();
		while(parentElement != null) {
			if(parentElement instanceof SectioningContent) sectioningLevel++;
			parentElement = parentElement.getParentElement();
		}
		// Highest tag is <h6>
		if(sectioningLevel > 6) throw new IOException("Sectioning exceeded depth of h6 (including page as h1): sectioningLevel = " + sectioningLevel);

		html.out.write('<');
		html.out.write(htmlElement);
		html.out.write("><h");
		char sectioningLevelChar = (char)('0' + sectioningLevel);
		html.out.write(sectioningLevelChar);
		String id = sectioningContent.getId();
		if(id != null) {
			html.out.write(" id=\"");
			PageIndex.appendIdInPage(
				pageIndex,
				sectioningContent.getPage(),
				id,
				new MediaWriter(textInXhtmlAttributeEncoder, html.out)
			);
			html.out.write('"');
		}
		html.out.write('>');
		html.text(sectioningContent.getLabel());
		html.out.write("</h");
		html.out.write(sectioningLevelChar);
		html.out.write('>');
		BufferResult body = sectioningContent.getBody();
		if(body.getLength() > 0) {
			html.out.write("<div class=\"semanticcms-section-h");
			html.out.write(sectioningLevelChar);
			html.out.write("-content\">");
			body.writeTo(new NodeBodyWriter(sectioningContent, html.out, context));
			html.out.write("</div>");
		}
		html.out.write("</");
		html.out.write(htmlElement);
		html.out.write('>');
	}

	public static void writeAside(
		Html html,
		ElementContext context,
		Aside aside,
		PageIndex pageIndex
	) throws IOException, ServletException, SkipPageException {
		writeSectioningContent(html, context, aside, "aside", pageIndex);
	}

	public static void writeNav(
		Html html,
		ElementContext context,
		Nav nav,
		PageIndex pageIndex
	) throws IOException, ServletException, SkipPageException {
		writeSectioningContent(html, context, nav, "nav", pageIndex);
	}

	public static void writeSection(
		Html html,
		ElementContext context,
		Section section,
		PageIndex pageIndex
	) throws IOException, ServletException, SkipPageException {
		writeSectioningContent(html, context, section, "section", pageIndex);
	}

	/**
	 * Make no instances.
	 */
	private SectionHtmlRenderer() {
	}
}
