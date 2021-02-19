/*
 * semanticcms-section-renderer-html - Sections rendered as HTML in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2019, 2020, 2021  AO Industries, Inc.
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
import com.aoindustries.html.Document;
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
import java.util.IdentityHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.SkipPageException;

// TODO: Implement with https://www.w3.org/TR/wai-aria-1.1/#aria-label
final public class SectionHtmlRenderer {

	private static final String TOC_DONE_PER_PAGE_REQUEST_ATTRIBUTE = SectionHtmlRenderer.class.getName() + ".tocDonePerPage";

	/**
	 * Writes the table of contents, if needed and not yet written on the page.
	 * The determination of whether needed on the page is only performed once per page, with the result cached in a
	 * request attribute.
	 */
	public static void writeToc(
		ServletRequest request,
		Document document,
		ElementContext context,
		Page page
	) throws Exception {
		@SuppressWarnings("unchecked")
		Map<Page,Boolean> tocDonePerPage = (Map<Page,Boolean>)request.getAttribute(TOC_DONE_PER_PAGE_REQUEST_ATTRIBUTE);
		if(tocDonePerPage == null) {
			tocDonePerPage = new IdentityHashMap<>();
			request.setAttribute(TOC_DONE_PER_PAGE_REQUEST_ATTRIBUTE, tocDonePerPage);
		}
		if(tocDonePerPage.putIfAbsent(page, true) == null) {
			context.include(
				"/semanticcms-section-renderer-html/toc.inc.jspx",
				document.out,
				Collections.singletonMap("page", page)
			);
		}
	}

	public static void writeSectioningContent(
		ServletRequest request,
		Document document,
		ElementContext context,
		SectioningContent sectioningContent,
		String htmlElement,
		PageIndex pageIndex
	) throws IOException, ServletException, SkipPageException {
		Page page = sectioningContent.getPage();
		if(page != null) {
			try {
				writeToc(request, document, context, page);
			} catch(Error | RuntimeException | IOException | ServletException | SkipPageException e) {
				throw e;
			} catch(Exception e) {
				throw new ServletException(e);
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

		document.out.write('<');
		document.out.write(htmlElement);
		String id = sectioningContent.getId();
		if(id != null) {
			document.out.write(" id=\"");
			PageIndex.appendIdInPage(
				pageIndex,
				page,
				id,
				new MediaWriter(document.encodingContext, textInXhtmlAttributeEncoder, document.out)
			);
			document.out.write('"');
		}
		document.out.write(" class=\"semanticcms-section\"><h");
		char sectioningLevelChar = (char)('0' + sectioningLevel);
		document.out.write(sectioningLevelChar);
		document.out.write('>');
		document.text(sectioningContent.getLabel());
		document.out.write("</h");
		document.out.write(sectioningLevelChar);
		document.out.write('>');
		BufferResult body = sectioningContent.getBody();
		if(body.getLength() > 0) {
			document.out.write("<div class=\"semanticcms-section-h");
			document.out.write(sectioningLevelChar);
			document.out.write("-content\">");
			body.writeTo(new NodeBodyWriter(sectioningContent, document.out, context));
			document.out.write("</div>");
		}
		document.out.write("</");
		document.out.write(htmlElement);
		document.out.write('>');
	}

	public static void writeAside(
		ServletRequest request,
		Document document,
		ElementContext context,
		Aside aside,
		PageIndex pageIndex
	) throws IOException, ServletException, SkipPageException {
		writeSectioningContent(request, document, context, aside, "aside", pageIndex);
	}

	public static void writeNav(
		ServletRequest request,
		Document document,
		ElementContext context,
		Nav nav,
		PageIndex pageIndex
	) throws IOException, ServletException, SkipPageException {
		writeSectioningContent(request, document, context, nav, "nav", pageIndex);
	}

	public static void writeSection(
		ServletRequest request,
		Document document,
		ElementContext context,
		Section section,
		PageIndex pageIndex
	) throws IOException, ServletException, SkipPageException {
		writeSectioningContent(request, document, context, section, "section", pageIndex);
	}

	/**
	 * Make no instances.
	 */
	private SectionHtmlRenderer() {
	}
}
