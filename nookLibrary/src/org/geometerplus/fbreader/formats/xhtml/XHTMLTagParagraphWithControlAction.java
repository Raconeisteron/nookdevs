/*
 * Copyright (C) 2007-2010 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.formats.xhtml;

import org.geometerplus.fbreader.bookmodel.BookReader;
import org.geometerplus.fbreader.bookmodel.FBTextKind;
import org.geometerplus.zlibrary.core.xml.ZLStringMap;

class XHTMLTagParagraphWithControlAction extends XHTMLTagAction {
    final byte myControl;

    XHTMLTagParagraphWithControlAction(byte control) {
        myControl = control;
    }

    protected void doAtStart(XHTMLReader reader, ZLStringMap xmlattributes) {
        final BookReader modelReader = reader.getModelReader();
        if ((myControl == FBTextKind.TITLE) &&
                (modelReader.Model.BookTextModel.getParagraphsNumber() > 1)) {
            modelReader.insertEndOfSectionParagraph();
        }
        modelReader.pushKind(myControl);
        modelReader.beginParagraph();
    }

    protected void doAtEnd(XHTMLReader reader) {
        final BookReader modelReader = reader.getModelReader();
        modelReader.endParagraph();
        modelReader.popKind();
    }
}
/*
void XHTMLTagParagraphWithControlAction::doAtStart(XHTMLReader &reader, const char**) {
	if ((myControl == TITLE) && (bookReader(reader).model().bookTextModel()->paragraphsNumber() > 1)) {
		bookReader(reader).insertEndOfSectionParagraph();
	}
	bookReader(reader).pushKind(myControl);
	bookReader(reader).beginParagraph();
}
*/
