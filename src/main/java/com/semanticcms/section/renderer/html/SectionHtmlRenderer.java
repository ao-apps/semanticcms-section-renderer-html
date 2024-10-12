/*
 * semanticcms-section-renderer-html - Sections rendered as HTML in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2019, 2020, 2021, 2022, 2024  AO Industries, Inc.
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
 * along with semanticcms-section-renderer-html.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.semanticcms.section.renderer.html;

import com.aoapps.html.any.AnyFlowContent;
import com.aoapps.html.any.AnyHeadingContent;
import com.aoapps.html.any.AnyPalpableContent;
import com.aoapps.html.any.AnySectioningContent;
import com.aoapps.html.any.NormalText;
import com.aoapps.io.buffer.BufferResult;
import com.aoapps.lang.io.function.IOFunction;
import com.aoapps.servlet.attribute.ScopeEE;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.renderer.html.PageIndex;
import com.semanticcms.section.model.Aside;
import com.semanticcms.section.model.Nav;
import com.semanticcms.section.model.Section;
import com.semanticcms.section.model.SectioningContent;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.SkipPageException;

/**
 * Writes sections as HTML.
 * <p>
 * TODO: Implement with https://www.w3.org/TR/wai-aria-1.1/#aria-label
 * </p>
 */
public final class SectionHtmlRenderer {

  /** Make no instances. */
  private SectionHtmlRenderer() {
    throw new AssertionError();
  }

  private static final ScopeEE.Request.Attribute<Map<Page, Boolean>> TOC_DONE_PER_PAGE_REQUEST_ATTRIBUTE =
      ScopeEE.REQUEST.attribute(SectionHtmlRenderer.class.getName() + ".tocDonePerPage");

  /**
   * Writes the table of contents, if needed and not yet written on the page.
   * The determination of whether needed on the page is only performed once per page, with the result cached in a
   * request attribute.
   */
  public static void writeToc(
      ServletRequest request,
      AnySectioningContent<?, ?> content,
      ElementContext context,
      Page page
  ) throws Exception {
    Map<Page, Boolean> tocDonePerPage = TOC_DONE_PER_PAGE_REQUEST_ATTRIBUTE.context(request)
        .computeIfAbsent(name -> new IdentityHashMap<>());
    if (tocDonePerPage.putIfAbsent(page, true) == null) {
      @SuppressWarnings("deprecation")
      Writer unsafe = content.getRawUnsafe();
      context.include(
          "/semanticcms-section-renderer-html/toc.inc.jspx",
          unsafe,
          Collections.singletonMap("page", page)
      );
    }
  }

  /**
   * Writes the given section.
   *
   * @param  content  {@link AnyPalpableContent} has both {@link AnyHeadingContent} and {@link AnySectioningContent}
   */
  public static void writeSectioningContent(
      ServletRequest request,
      AnyPalpableContent<?, ?> content,
      ElementContext context,
      SectioningContent sectioningContent,
      IOFunction<AnySectioningContent<?, ?>, NormalText<?, ?, ?, ? extends AnyFlowContent<?, ?>, ?>> htmlElement,
      PageIndex pageIndex
  ) throws IOException, ServletException, SkipPageException {
    Page page = sectioningContent.getPage();
    if (page != null) {
      try {
        writeToc(request, content, context, page);
      } catch (Error | RuntimeException | IOException | ServletException | SkipPageException e) {
        throw e;
      } catch (Exception e) {
        throw new ServletException(e);
      }
    }
    // Count the sectioning level by finding all sectioning contents in the parent elements
    int sectioningLevel;
      {
        int sectioningLevelTmp = 2; // <h1> is reserved for page titles
        com.semanticcms.core.model.Element parentElement = sectioningContent.getParentElement();
        while (parentElement != null) {
          if (parentElement instanceof SectioningContent) {
            sectioningLevelTmp++;
          }
          parentElement = parentElement.getParentElement();
        }
        // Highest tag is <h6>
        if (sectioningLevelTmp > 6) {
          throw new IOException("Sectioning exceeded depth of h6 (including page as h1): sectioningLevel = " + sectioningLevelTmp);
        }
        sectioningLevel = sectioningLevelTmp;
      }

    String id = sectioningContent.getId();
    htmlElement.apply(content)
        .id((id == null) ? null : idAttr -> PageIndex.appendIdInPage(
            pageIndex,
            page,
            id,
            idAttr
        ))
        .clazz("semanticcms-section")
        .__(section -> {
          section.h__(sectioningLevel, sectioningContent);
          BufferResult body = sectioningContent.getBody();
          if (body.getLength() > 0) {
            section.div().clazz(clazz -> clazz.append("semanticcms-section-h").append((char) ('0' + sectioningLevel)).append("-content")).__(div -> {
              @SuppressWarnings("deprecation")
              Writer unsafe = div.getRawUnsafe();
              body.writeTo(new NodeBodyWriter(sectioningContent, unsafe, context));
            });
          }
        });
  }

  /**
   * Writes the given aside.
   */
  public static void writeAside(
      ServletRequest request,
      AnyPalpableContent<?, ?> content,
      ElementContext context,
      Aside aside,
      PageIndex pageIndex
  ) throws IOException, ServletException, SkipPageException {
    writeSectioningContent(request, content, context, aside, AnySectioningContent::aside, pageIndex);
  }

  /**
   * Writes the given nav.
   */
  public static void writeNav(
      ServletRequest request,
      AnyPalpableContent<?, ?> content,
      ElementContext context,
      Nav nav,
      PageIndex pageIndex
  ) throws IOException, ServletException, SkipPageException {
    writeSectioningContent(request, content, context, nav, AnySectioningContent::nav, pageIndex);
  }

  /**
   * Writes the given section.
   */
  public static void writeSection(
      ServletRequest request,
      AnyPalpableContent<?, ?> content,
      ElementContext context,
      Section section,
      PageIndex pageIndex
  ) throws IOException, ServletException, SkipPageException {
    writeSectioningContent(request, content, context, section, AnySectioningContent::section, pageIndex);
  }
}
