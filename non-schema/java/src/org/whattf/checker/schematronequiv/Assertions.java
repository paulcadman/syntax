/*
 * Copyright (c) 2008 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package org.whattf.checker.schematronequiv;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.whattf.checker.AttributeUtil;
import org.whattf.checker.Checker;
import org.whattf.checker.LocatorImpl;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class Assertions extends Checker {

    public static boolean lowerCaseLiteralEqualsIgnoreAsciiCaseString(
            String lowerCaseLiteral, String string) {
        if (string == null) {
            return false;
        }
        if (lowerCaseLiteral.length() != string.length()) {
            return false;
        }
        for (int i = 0; i < lowerCaseLiteral.length(); i++) {
            char c0 = lowerCaseLiteral.charAt(i);
            char c1 = string.charAt(i);
            if (c1 >= 'A' && c1 <= 'Z') {
                c1 += 0x20;
            }
            if (c0 != c1) {
                return false;
            }
        }
        return true;
    }

    private static final Set<String> OBSOLETE_ELEMENTS = new HashSet<String>();

    static {
        OBSOLETE_ELEMENTS.add("center");
        OBSOLETE_ELEMENTS.add("font");
        OBSOLETE_ELEMENTS.add("big");
        OBSOLETE_ELEMENTS.add("s");
        OBSOLETE_ELEMENTS.add("strike");
        OBSOLETE_ELEMENTS.add("tt");
        OBSOLETE_ELEMENTS.add("u");
        OBSOLETE_ELEMENTS.add("acronym");
        OBSOLETE_ELEMENTS.add("dir");
        OBSOLETE_ELEMENTS.add("applet");
    }

    private static final String[] SPECIAL_ANCESTORS = { "a", "address", "bb",
            "button", "dfn", "footer", "form", "header", "label", "map",
            "noscript" };

    private static int specialAncestorNumber(String name) {
        for (int i = 0; i < SPECIAL_ANCESTORS.length; i++) {
            if (name == SPECIAL_ANCESTORS[i]) {
                return i;
            }
        }
        return -1;
    }

    private static Map<String, Integer> ANCESTOR_MASK_BY_DESCENDANT = new HashMap<String, Integer>();

    private static void registerProhibitedAncestor(String ancestor,
            String descendant) {
        int number = specialAncestorNumber(ancestor);
        if (number == -1) {
            throw new IllegalStateException("Ancestor not found in array: " + ancestor);
        }
        Integer maskAsObject = ANCESTOR_MASK_BY_DESCENDANT.get(descendant);
        int mask = 0;
        if (maskAsObject != null) {
            mask = maskAsObject.intValue();
        }
        mask |= (1 << number);
        ANCESTOR_MASK_BY_DESCENDANT.put(descendant, new Integer(mask));
    }

    static {
        registerProhibitedAncestor("form", "form");
        registerProhibitedAncestor("dfn", "dfn");
        registerProhibitedAncestor("noscript", "noscript");
        registerProhibitedAncestor("label", "label");
        registerProhibitedAncestor("address", "address");
        registerProhibitedAncestor("header", "blockquote");
        registerProhibitedAncestor("address", "blockquote");
        registerProhibitedAncestor("header", "section");
        registerProhibitedAncestor("footer", "section");
        registerProhibitedAncestor("address", "section");
        registerProhibitedAncestor("header", "nav");
        registerProhibitedAncestor("footer", "nav");
        registerProhibitedAncestor("address", "nav");
        registerProhibitedAncestor("header", "article");
        registerProhibitedAncestor("footer", "article");
        registerProhibitedAncestor("address", "article");
        registerProhibitedAncestor("header", "aside");
        registerProhibitedAncestor("footer", "aside");
        registerProhibitedAncestor("address", "aside");
        registerProhibitedAncestor("header", "header");
        registerProhibitedAncestor("footer", "header");
        registerProhibitedAncestor("address", "header");
        registerProhibitedAncestor("header", "footer");
        registerProhibitedAncestor("footer", "footer");
        registerProhibitedAncestor("address", "footer");
        registerProhibitedAncestor("footer", "h1");
        registerProhibitedAncestor("footer", "h2");
        registerProhibitedAncestor("footer", "h3");
        registerProhibitedAncestor("footer", "h4");
        registerProhibitedAncestor("footer", "h5");
        registerProhibitedAncestor("footer", "h6");
        registerProhibitedAncestor("address", "h1");
        registerProhibitedAncestor("address", "h2");
        registerProhibitedAncestor("address", "h3");
        registerProhibitedAncestor("address", "h4");
        registerProhibitedAncestor("address", "h5");
        registerProhibitedAncestor("address", "h6");
        registerProhibitedAncestor("a", "a");
        registerProhibitedAncestor("button", "a");
        registerProhibitedAncestor("bb", "a");
        registerProhibitedAncestor("a", "datagrid");
        registerProhibitedAncestor("button", "datagrid");
        registerProhibitedAncestor("bb", "datagrid");
        registerProhibitedAncestor("a", "details");
        registerProhibitedAncestor("button", "details");
        registerProhibitedAncestor("bb", "details");
        registerProhibitedAncestor("a", "button");
        registerProhibitedAncestor("button", "button");
        registerProhibitedAncestor("bb", "button");
        registerProhibitedAncestor("a", "textarea");
        registerProhibitedAncestor("button", "textarea");
        registerProhibitedAncestor("bb", "textarea");
        registerProhibitedAncestor("a", "select");
        registerProhibitedAncestor("button", "select");
        registerProhibitedAncestor("bb", "select");
    }

    private static final int A_BUTTON_BB_MASK = (1 << specialAncestorNumber("a"))
            | (1 << specialAncestorNumber("button"))
            | (1 << specialAncestorNumber("bb"));

    private static final int MAP_MASK = (1 << specialAncestorNumber("map"));

    private static final int HREF_MASK = (1 << 30);

    private static final int REPEAT_MASK = (1 << 29);

    private static final Map<String, Set<String>> REQUIRED_ROLE_PARENT_BY_CHILD = new HashMap<String, Set<String>>();

    private static void registerRequiredParentRole(String parent, String child) {
        Set<String> parents = REQUIRED_ROLE_PARENT_BY_CHILD.get(child);
        if (parents == null) {
            parents = new HashSet<String>();
        }
        parents.add(parent);
        REQUIRED_ROLE_PARENT_BY_CHILD.put(child, parents);
    }

    static {
        registerRequiredParentRole("listbox", "option");
        registerRequiredParentRole("combobox", "option");
        registerRequiredParentRole("menu", "menuitem");
        registerRequiredParentRole("menu", "menuitemcheckbox");
        registerRequiredParentRole("menu", "menuitemradio");
        registerRequiredParentRole("tablist", "tab");
        registerRequiredParentRole("tree", "treeitem");
        registerRequiredParentRole("list", "listitem");
        registerRequiredParentRole("row", "gridcell");
    }

    private static final Set<String> MUST_NOT_DANGLE_IDREFS = new HashSet<String>();

    static {
        MUST_NOT_DANGLE_IDREFS.add("aria-controls");
        MUST_NOT_DANGLE_IDREFS.add("aria-describedby");
        MUST_NOT_DANGLE_IDREFS.add("aria-flowto");
        MUST_NOT_DANGLE_IDREFS.add("aria-labelledby");
        MUST_NOT_DANGLE_IDREFS.add("aria-owns");
    }

    private static final Map<String, Set<String>> ALLOWED_CHILD_ROLE_BY_PARENT = new HashMap<String, Set<String>>();

    private static void registerAllowedChildRole(String parent, String child) {
        Set<String> children = ALLOWED_CHILD_ROLE_BY_PARENT.get(parent);
        if (children == null) {
            children = new HashSet<String>();
        }
        children.add(child);
        ALLOWED_CHILD_ROLE_BY_PARENT.put(parent, children);
    }

    static {
        registerAllowedChildRole("listbox", "option");
        registerAllowedChildRole("combobox", "option");
        registerAllowedChildRole("menu", "menuitem");
        registerAllowedChildRole("menu", "menuitemcheckbox");
        registerAllowedChildRole("menu", "menuitemradio");
        registerAllowedChildRole("tree", "treeitem");
        registerAllowedChildRole("list", "listitem");
        registerAllowedChildRole("radiogroup", "radio");
    }

    private class IdrefLocator {
        private final Locator locator;

        private final String idref;

        private final String additional;

        /**
         * @param locator
         * @param idref
         */
        public IdrefLocator(Locator locator, String idref) {
            this.locator = new LocatorImpl(locator);
            this.idref = idref;
            this.additional = null;
        }

        public IdrefLocator(Locator locator, String idref, String additional) {
            this.locator = new LocatorImpl(locator);
            this.idref = idref;
            this.additional = additional;
        }

        /**
         * Returns the locator.
         * 
         * @return the locator
         */
        public Locator getLocator() {
            return locator;
        }

        /**
         * Returns the idref.
         * 
         * @return the idref
         */
        public String getIdref() {
            return idref;
        }

        /**
         * Returns the additional.
         * 
         * @return the additional
         */
        public String getAdditional() {
            return additional;
        }
    }

    private class StackNode {
        private final int ancestorMask;

        private final String name; // null if not HTML

        private final String role;

        private final String activeDescendant;
        
        private boolean headingDescendants = false;

        private boolean children = false;

        private boolean selectedOptions = false;

        private String datagridFirstChild = null;

        /**
         * Returns the datagridFirstChild.
         * 
         * @return the datagridFirstChild
         */
        public String getDatagridFirstChild() {
            return datagridFirstChild;
        }

        /**
         * Sets the datagridFirstChild.
         * 
         * @param datagridFirstChild
         *            the datagridFirstChild to set
         */
        public void setDatagridFirstChild(String datagridFirstChild) {
            this.datagridFirstChild = datagridFirstChild;
        }

        /**
         * @param ancestorMask
         */
        public StackNode(int ancestorMask, String name, String role, String activeDescendant) {
            this.ancestorMask = ancestorMask;
            this.name = name;
            this.role = role;
            this.activeDescendant = activeDescendant;
        }

        /**
         * Returns the ancestorMask.
         * 
         * @return the ancestorMask
         */
        public int getAncestorMask() {
            return ancestorMask;
        }

        /**
         * Returns the headingDescendants.
         * 
         * @return the headingDescendants
         */
        public boolean isHeadingDescendants() {
            return headingDescendants;
        }

        /**
         * Sets the headingDescendants.
         * 
         * @param headingDescendants
         *            the headingDescendants to set
         */
        public void setHeadingDescendants() {
            this.headingDescendants = true;
        }

        /**
         * Returns the name.
         * 
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the children.
         * 
         * @return the children
         */
        public boolean isChildren() {
            return children;
        }

        /**
         * Sets the children.
         * 
         * @param children
         *            the children to set
         */
        public void setChildren() {
            this.children = true;
        }

        /**
         * Returns the selectedOptions.
         * 
         * @return the selectedOptions
         */
        public boolean isSelectedOptions() {
            return selectedOptions;
        }

        /**
         * Sets the selectedOptions.
         * 
         * @param selectedOptions
         *            the selectedOptions to set
         */
        public void setSelectedOptions() {
            this.selectedOptions = true;
        }

        /**
         * Returns the role.
         * 
         * @return the role
         */
        public String getRole() {
            return role;
        }

        /**
         * Returns the activeDescendant.
         * 
         * @return the activeDescendant
         */
        public String getActiveDescendant() {
            return activeDescendant;
        }
    }

    private StackNode[] stack;

    private int currentPtr;

    public Assertions() {
        super();
    }
    
    private void push(StackNode node) {
        currentPtr++;
        if (currentPtr == stack.length) {
            StackNode[] newStack = new StackNode[stack.length + 64];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }
        stack[currentPtr] = node;
    }

    private StackNode pop() {
        return stack[currentPtr--];
    }

    private StackNode peek() {
        return stack[currentPtr];
    }

    private Map<StackNode, Locator> openHeaders = new HashMap<StackNode, Locator>();

    private Map<StackNode, Locator> openSingleSelects = new HashMap<StackNode, Locator>();

    private Map<StackNode, Locator> openActiveDescendants = new HashMap<StackNode, Locator>();
    
    private LinkedHashSet<IdrefLocator> contextmenuReferences = new LinkedHashSet<IdrefLocator>();

    private Set<String> menuIds = new HashSet<String>();

    private LinkedHashSet<IdrefLocator> repeatTemplateReferences = new LinkedHashSet<IdrefLocator>();

    private Set<String> templateIds = new HashSet<String>();

    private LinkedHashSet<IdrefLocator> formControlReferences = new LinkedHashSet<IdrefLocator>();

    private Set<String> formControlIds = new HashSet<String>();

    private LinkedHashSet<IdrefLocator> inputAddReferences = new LinkedHashSet<IdrefLocator>();

    private LinkedHashSet<IdrefLocator> buttonAddReferences = new LinkedHashSet<IdrefLocator>();

    private LinkedHashSet<IdrefLocator> listReferences = new LinkedHashSet<IdrefLocator>();

    private Set<String> listIds = new HashSet<String>();

    private LinkedHashSet<IdrefLocator> ariaReferences = new LinkedHashSet<IdrefLocator>();

    private Set<String> allIds = new HashSet<String>();

    /**
     * @see org.whattf.checker.Checker#endDocument()
     */
    @Override public void endDocument() throws SAXException {
        // contextmenu
        for (IdrefLocator idrefLocator : contextmenuReferences) {
            if (!menuIds.contains(idrefLocator.getIdref())) {
                err(
                        "The \u201Ccontextmenu\u201D attribute must refer to a \u201Cmenu\u201D element.",
                        idrefLocator.getLocator());
            }
        }

        // repeat-template
        for (IdrefLocator idrefLocator : repeatTemplateReferences) {
            if (!templateIds.contains(idrefLocator.getIdref())) {
                err(
                        "The \u201Crepeat-template\u201D attribute must refer to a repetition template.",
                        idrefLocator.getLocator());
            }
        }

        // label for
        for (IdrefLocator idrefLocator : formControlReferences) {
            if (!formControlIds.contains(idrefLocator.getIdref())) {
                err(
                        "The \u201Cfor\u201D attribute of the \u201Clabel\u201D element must refer to a form control.",
                        idrefLocator.getLocator());
            }
        }

        // type=add
        for (IdrefLocator idrefLocator : inputAddReferences) {
            if (!templateIds.contains(idrefLocator.getIdref())) {
                err(
                        "The \u201Ctemplate\u201D attribute of an \u201Cinput\u201D element that is of \u201Ctype=\"add\"\u201D must refer to a repetition template.",
                        idrefLocator.getLocator());
            }
        }
        for (IdrefLocator idrefLocator : buttonAddReferences) {
            if (!templateIds.contains(idrefLocator.getIdref())) {
                err(
                        "The \u201Ctemplate\u201D attribute of an \u201Cbutton\u201D element that is of \u201Ctype=\"add\"\u201D must refer to a repetition template.",
                        idrefLocator.getLocator());
            }
        }

        // input list
        for (IdrefLocator idrefLocator : listReferences) {
            if (!listIds.contains(idrefLocator.getIdref())) {
                err(
                        "The \u201Clist\u201D attribute of the \u201Cinput\u201D element must refer to a \u201Cdatalist\u201D element or to a \u201Cselect\u201D element.",
                        idrefLocator.getLocator());
            }
        }

        // ARIA idrefs
        for (IdrefLocator idrefLocator : ariaReferences) {
            if (!allIds.contains(idrefLocator.getIdref())) {
                err(
                        "The \u201C"
                                + idrefLocator.getAdditional()
                                + "\u201D attribute must point to an element in the same document.",
                        idrefLocator.getLocator());
            }
        }
        
        clearCollections();
        stack = null;
    }

    private static double getDoubleAttribute(Attributes atts, String name) {
        String str = atts.getValue("", name);
        if (str == null) {
            return Double.NaN;
        } else {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
    }

    /**
     * @see org.whattf.checker.Checker#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override public void endElement(String uri, String localName, String name)
            throws SAXException {
        StackNode node = pop();
        Locator locator = null;
        if ((locator = openHeaders.remove(node)) != null) {
            if (!node.isHeadingDescendants()) {
                err(
                        "The \u201Cheader\u201D element must have at least one \u201Ch1\u201D\u2013\u201Ch6\u201D descendant.",
                        locator);
            }
        }
        openSingleSelects.remove(node);
        if ((locator = openActiveDescendants.remove(node)) != null) {
            err("The \u201Caria-activedescendant\u201D attribute must refer to a descendant element.", locator);
        }
    }

    /**
     * @see org.whattf.checker.Checker#startDocument()
     */
    @Override public void startDocument() throws SAXException {
        clearCollections();
        stack = new StackNode[32];
        currentPtr = 0;
        stack[0] = null;
    }

    private void clearCollections() {
        openHeaders.clear();
        openSingleSelects.clear();
        openActiveDescendants.clear();
        contextmenuReferences.clear();
        menuIds.clear();
        repeatTemplateReferences.clear();
        templateIds.clear();
        formControlReferences.clear();
        formControlIds.clear();
        inputAddReferences.clear();
        buttonAddReferences.clear();
        listReferences.clear();
        listIds.clear();
        ariaReferences.clear();
        allIds.clear();        
    }

    /**
     * @see org.whattf.checker.Checker#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override public void startElement(String uri, String localName,
            String name, Attributes atts) throws SAXException {
        boolean skipDatagridCheck = false;
        Set<String> ids = new HashSet<String>();
        String role = null;
        String activeDescendant = null;
        boolean href = false;
        boolean repeat = false;

        StackNode parent = peek();
        int ancestorMask = 0;
        String parentRole = null;
        String parentName = null;
        if (parent != null) {
            ancestorMask = parent.getAncestorMask();
            parentName = parent.getName();
            parentRole = parent.getRole();
        }
        if ("http://www.w3.org/1999/xhtml" == uri) {
            boolean controls = false;
            boolean hidden = false;
            boolean add = false;
            boolean toolbar = false;
            boolean ismap = false;
            boolean isTemplate = false;
            boolean selected = false;
            boolean remove = false;
            boolean moveUp = false;
            boolean moveDown = false;
            String templateRef = null;
            String xmlLang = null;
            String lang = null;
            String id = null;
            String contextmenu = null;
            String repeatTemplate = null;
            String list = null;

            int len = atts.getLength();
            for (int i = 0; i < len; i++) {
                String attUri = atts.getURI(i);
                if (attUri.length() == 0) {
                    String attLocal = atts.getLocalName(i);
                    if ("href" == attLocal) {
                        href = true;
                    } else if ("controls" == attLocal) {
                        controls = true;
                    } else if ("type" == attLocal) {
                        String attValue = atts.getValue(i);
                        if (lowerCaseLiteralEqualsIgnoreAsciiCaseString(
                                "hidden", attValue)) {
                            hidden = true;
                        } else if (lowerCaseLiteralEqualsIgnoreAsciiCaseString(
                                "toolbar", attValue)) {
                            toolbar = true;
                        } else if (lowerCaseLiteralEqualsIgnoreAsciiCaseString(
                                "add", attValue)) {
                            add = true;
                        } else if (lowerCaseLiteralEqualsIgnoreAsciiCaseString(
                                "remove", attValue)) {
                            remove = true;
                        } else if (lowerCaseLiteralEqualsIgnoreAsciiCaseString(
                                "move-up", attValue)) {
                            moveUp = true;
                        } else if (lowerCaseLiteralEqualsIgnoreAsciiCaseString(
                                "move-down", attValue)) {
                            moveDown = true;
                        }
                    } else if ("role" == attLocal) {
                        role = atts.getValue(i);
                    } else if ("aria-activedescendant" == attLocal) {
                        activeDescendant = atts.getValue(i);
                    } else if ("template" == attLocal) {
                        templateRef = atts.getValue(i);
                    } else if ("list" == attLocal) {
                        list = atts.getValue(i);
                    } else if ("lang" == attLocal) {
                        lang = atts.getValue(i);
                    } else if ("id" == attLocal) {
                        id = atts.getValue(i);
                    } else if ("contextmenu" == attLocal) {
                        contextmenu = atts.getValue(i);
                    } else if ("repeat-template" == attLocal) {
                        repeatTemplate = atts.getValue(i);
                    } else if ("ismap" == attLocal) {
                        ismap = true;
                    } else if ("selected" == attLocal) {
                        selected = true;
                    } else if ("repeat" == attLocal) {
                        repeat = true;
                        String attValue = atts.getValue(i);
                        if (lowerCaseLiteralEqualsIgnoreAsciiCaseString(
                                "template", attValue)) {
                            isTemplate = true;
                        }
                    }
                } else if ("http://www.w3.org/XML/1998/namespace" == attUri) {
                    if ("lang" == atts.getValue(i)) {
                        xmlLang = atts.getValue(i);
                    }
                }

                if (atts.getType(i) == "ID") {
                    String attVal = atts.getValue(i);
                    if (attVal.length() != 0) {
                        ids.add(attVal);
                    }
                }
            }

            // Obsolete elements
            if (OBSOLETE_ELEMENTS.contains(localName)) {
                err("The \u201C" + localName + "\u201D element is obsolete.");
            }

            // Exclusions
            Integer maskAsObject;
            int mask = 0;
            String descendantUiString = "";
            if ((maskAsObject = ANCESTOR_MASK_BY_DESCENDANT.get(localName)) != null) {
                mask = maskAsObject.intValue();
                descendantUiString = localName;
            } else if ("video" == localName && controls) {
                mask = A_BUTTON_BB_MASK;
                descendantUiString = "video controls";
            } else if ("audio" == localName && controls) {
                mask = A_BUTTON_BB_MASK;
                descendantUiString = "audio controls";
            } else if ("menu" == localName && toolbar) {
                mask = A_BUTTON_BB_MASK;
                descendantUiString = "menu type=toolbar";
            } else if ("input" == localName && !hidden) {
                mask = A_BUTTON_BB_MASK;
                descendantUiString = "input";
            }
            if (mask != 0) {
                int maskHit = ancestorMask & mask;
                if (maskHit != 0) {
                    for (int j = 0; j < SPECIAL_ANCESTORS.length; j++) {
                        if ((maskHit & 1) != 0) {
                            err("The element \u201C"
                                    + descendantUiString
                                    + "\u201D must not appear as a descendant of the \u201C"
                                    + SPECIAL_ANCESTORS[j] + "\u201D element.");
                        }
                        maskHit >>= 1;
                    }
                }
            }

            // Required ancestors
            if ("area" == localName && ((ancestorMask & MAP_MASK) == 0)) {
                err("The \u201Carea\u201D element must have a \u201Cmap\u201D ancestor.");
            } else if ("img" == localName && ismap
                    && ((ancestorMask & HREF_MASK) == 0)) {
                err("The \u201Cimg\u201D element with the \u201Cismap\u201D attribute set must have an \u201Ca\u201D ancestor with the \u201Chref\u201D attribute.");
            }

            // at least 1 h1-h6 in header
            else if ("h1" == localName || "h2" == localName
                    || "h3" == localName || "h4" == localName
                    || "h5" == localName || "h6" == localName) {
                for (Map.Entry<StackNode, Locator> entry : openHeaders.entrySet()) {
                    entry.getKey().setHeadingDescendants();
                }
            }

            // progress
            else if ("progress" == localName) {
                double value = getDoubleAttribute(atts, "value");
                if (!Double.isNaN(value)) {
                    double max = getDoubleAttribute(atts, "max");
                    if (Double.isNaN(max)) {
                        if (!(value <= 1.0)) {
                            err("The value of the  \u201Cvalue\u201D attribute must be less than or equal to one when the \u201Cmax\u201D attribute is absent.");
                        }
                    } else {
                        if (!(value <= max)) {
                            err("The value of the  \u201Cvalue\u201D attribute must be less than or equal to the value of the \u201Cmax\u201D attribute.");
                        }
                    }
                }
            }

            // meter
            else if ("meter" == localName) {
                double value = getDoubleAttribute(atts, "value");
                double min = getDoubleAttribute(atts, "min");
                double max = getDoubleAttribute(atts, "max");
                double optimum = getDoubleAttribute(atts, "optimum");
                double low = getDoubleAttribute(atts, "low");
                double high = getDoubleAttribute(atts, "high");

                if (!Double.isNaN(min) && !Double.isNaN(value)
                        && !(min <= value)) {
                    err("The value of the \u201Cmin\u201D attribute must be less than or equal to the value of the \u201Cvalue\u201D attribute.");
                }
                if (Double.isNaN(min) && !Double.isNaN(value) && !(0 <= value)) {
                    err("The value of the \u201Cvalue\u201D attribute must be greater than or equal to zero when the \u201Cmin\u201D attribute is absent.");
                }
                if (!Double.isNaN(value) && !Double.isNaN(max)
                        && !(value <= max)) {
                    err("The value of the \u201Cvalue\u201D attribute must be less than or equal to the value of the \u201Cmax\u201D attribute.");
                }
                if (!Double.isNaN(value) && Double.isNaN(max) && !(value <= 1)) {
                    err("The value of the \u201Cvalue\u201D attribute must be less than or equal to one when the \u201Cmax\u201D attribute is absent.");
                }
                if (!Double.isNaN(min) && !Double.isNaN(max) && !(min <= max)) {
                    err("The value of the \u201Cmin\u201D attribute must be less than or equal to the value of the \u201Cmax\u201D attribute.");
                }
                if (Double.isNaN(min) && !Double.isNaN(max) && !(0 <= max)) {
                    err("The value of the \u201Cmax\u201D attribute must be greater than or equal to zero when the \u201Cmin\u201D attribute is absent.");
                }
                if (!Double.isNaN(min) && Double.isNaN(max) && !(min <= 1)) {
                    err("The value of the \u201Cmin\u201D attribute must be less than or equal to one when the \u201Cmax\u201D attribute is absent.");
                }
                if (!Double.isNaN(min) && !Double.isNaN(low) && !(min <= low)) {
                    err("The value of the \u201Cmin\u201D attribute must be less than or equal to the value of the \u201Clow\u201D attribute.");
                }
                if (Double.isNaN(min) && !Double.isNaN(low) && !(0 <= low)) {
                    err("The value of the \u201Clow\u201D attribute must be greater than or equal to zero when the \u201Cmin\u201D attribute is absent.");
                }
                if (!Double.isNaN(min) && !Double.isNaN(high) && !(min <= high)) {
                    err("The value of the \u201Cmin\u201D attribute must be less than or equal to the value of the \u201Chigh\u201D attribute.");
                }
                if (Double.isNaN(min) && !Double.isNaN(high) && !(0 <= high)) {
                    err("The value of the \u201Chigh\u201D attribute must be greater than or equal to zero when the \u201Cmin\u201D attribute is absent.");
                }
                if (!Double.isNaN(low) && !Double.isNaN(high) && !(low <= high)) {
                    err("The value of the \u201Clow\u201D attribute must be less than or equal to the value of the \u201Chigh\u201D attribute.");
                }
                if (!Double.isNaN(high) && !Double.isNaN(max) && !(high <= max)) {
                    err("The value of the \u201Chigh\u201D attribute must be less than or equal to the value of the \u201Cmax\u201D attribute.");
                }
                if (!Double.isNaN(high) && Double.isNaN(max) && !(high <= 1)) {
                    err("The value of the \u201Chigh\u201D attribute must be less than or equal to one when the \u201Cmax\u201D attribute is absent.");
                }
                if (!Double.isNaN(low) && !Double.isNaN(max) && !(low <= max)) {
                    err("The value of the \u201Clow\u201D attribute must be less than or equal to the value of the \u201Cmax\u201D attribute.");
                }
                if (!Double.isNaN(low) && Double.isNaN(max) && !(low <= 1)) {
                    err("The value of the \u201Clow\u201D attribute must be less than or equal to one when the \u201Cmax\u201D attribute is absent.");
                }
                if (!Double.isNaN(min) && !Double.isNaN(optimum)
                        && !(min <= optimum)) {
                    err("The value of the \u201Cmin\u201D attribute must be less than or equal to the value of the \u201Coptimum\u201D attribute.");
                }
                if (Double.isNaN(min) && !Double.isNaN(optimum)
                        && !(0 <= optimum)) {
                    err("The value of the \u201Coptimum\u201D attribute must be greater than or equal to zero when the \u201Cmin\u201D attribute is absent.");
                }
                if (!Double.isNaN(optimum) && !Double.isNaN(max)
                        && !(optimum <= max)) {
                    err("The value of the \u201Coptimum\u201D attribute must be less than or equal to the value of the \u201Cmax\u201D attribute.");
                }
                if (!Double.isNaN(optimum) && Double.isNaN(max)
                        && !(optimum <= 1)) {
                    err("The value of the \u201Coptimum\u201D attribute must be less than or equal to one when the \u201Cmax\u201D attribute is absent.");
                }
            }

            // encoding decl
            else if ("meta" == localName
                    && currentPtr == 2
                    && stack[2].getName() == "head"
                    && stack[1].getName() == "html"
                    && (atts.getIndex("", "charset") > -1 || lowerCaseLiteralEqualsIgnoreAsciiCaseString(
                            "content-type", atts.getValue("", "http-equiv")))
                    && stack[2].isChildren()) {
                err("The internal character encoding declaration must be the first child of the \u201Chead\u201D element.");
            }

            // map required attrs
            else if ("map" == localName && id != null) {
                String nameVal = atts.getValue("", "name");
                if (nameVal != null && !nameVal.equals(id)) {
                    err("The \u201Cid\u201D attribute on a \u201Cmap\u201D element must have an the same value as the \u201Cname\u201D attribute.");
                }
            }

            // bdo required attrs
            else if ("bdo" == localName && atts.getIndex("", "dir") < 0) {
                err("A \u201Cbdo\u201D element must have an \u201Cdir\u201D attribute.");
            }

            // datagrid silliness
            else if (parentName == "datagrid"
                    && !parent.isChildren()
                    && ("table" == localName || "select" == localName || "datalist" == localName)) {
                parent.setDatagridFirstChild(localName);
                skipDatagridCheck = true;
            }

            // lang and xml:lang for XHTML5
            if (lang != null && (xmlLang == null || !lang.equals(xmlLang))) {
                err("When the attribute \u201Clang\u201D is specified, the element must also have the attribute \u201Clang\u201D in the XML namespace present with the same value.");
            }

            // contextmenu
            if (contextmenu != null) {
                contextmenuReferences.add(new IdrefLocator(new LocatorImpl(
                        getDocumentLocator()), contextmenu));
            }
            if ("menu" == localName) {
                menuIds.addAll(ids);
            }

            // repeat-template
            if (repeatTemplate != null) {
                repeatTemplateReferences.add(new IdrefLocator(new LocatorImpl(
                        getDocumentLocator()), repeatTemplate));
            }
            if (isTemplate) {
                templateIds.addAll(ids);
            }

            // label for
            if ("label" == localName) {
                String forVal = atts.getValue("", "for");
                if (forVal != null) {
                    formControlReferences.add(new IdrefLocator(new LocatorImpl(
                            getDocumentLocator()), forVal));
                }
            }
            if (("input" == localName && !hidden) || "textarea" == localName
                    || "select" == localName || "button" == localName
                    || "output" == localName) {
                formControlIds.addAll(ids);
            }

            // type=add
            if ("input" == localName && templateRef != null && add) {
                inputAddReferences.add(new IdrefLocator(new LocatorImpl(
                        getDocumentLocator()), repeatTemplate));
            }
            if ("button" == localName && templateRef != null && add) {
                buttonAddReferences.add(new IdrefLocator(new LocatorImpl(
                        getDocumentLocator()), repeatTemplate));
            }

            // input list
            if ("input" == localName && list != null) {
                listReferences.add(new IdrefLocator(new LocatorImpl(
                        getDocumentLocator()), list));
            }
            if (isTemplate) {
                listIds.addAll(ids);
            }

            // multiple selected options
            if ("option" == localName && selected) {
                for (Map.Entry<StackNode, Locator> entry : openSingleSelects.entrySet()) {
                    StackNode node = entry.getKey();
                    if (node.isSelectedOptions()) {
                        err("The \u201Cselect\u201D element cannot have more than one selected \u201Coption\u201D descendant unless the \u201Cmultiple\u201D attribute is specified.");
                    } else {
                        node.setSelectedOptions();
                    }
                }
            }

            // move ancestors
            if ((ancestorMask & REPEAT_MASK) == 0) {
                if ("input" == localName) {
                    if (remove) {
                        err("An \u201Cinput\u201D element of \u201Ctype=\"remove\"\u201D must have a repetition block or a repetition template as an ancestor.");
                    } else if (moveUp) {
                        err("An \u201Cinput\u201D element of \u201Ctype=\"move-up\"\u201D must have a repetition block or a repetition template as an ancestor.");
                    } else if (moveDown) {
                        err("An \u201Cinput\u201D element of \u201Ctype=\"move-down\"\u201D must have a repetition block or a repetition template as an ancestor.");
                    }
                } else if ("button" == localName) {
                    if (remove) {
                        err("A \u201Cbutton\u201D element of \u201Ctype=\"remove\"\u201D must have a repetition block or a repetition template as an ancestor.");
                    } else if (moveUp) {
                        err("A \u201Cbutton\u201D element of \u201Ctype=\"move-up\"\u201D must have a repetition block or a repetition template as an ancestor.");
                    } else if (moveDown) {
                        err("A \u201Cbutton\u201D element of \u201Ctype=\"move-down\"\u201D must have a repetition block or a repetition template as an ancestor.");
                    }
                }
            }

        } else {
            int len = atts.getLength();
            for (int i = 0; i < len; i++) {
                if (atts.getType(i) == "ID") {
                    String attVal = atts.getValue(i);
                    if (attVal.length() != 0) {
                        ids.add(attVal);
                    }
                }
                String attLocal = atts.getLocalName(i);
                if (atts.getURI(i).length() == 0) {
                    if ("role" == attLocal) {
                        role = atts.getValue(i);
                    } else if ("aria-activedescendant" == attLocal) {
                        activeDescendant = atts.getValue(i);
                    }
                }
            }

            allIds.addAll(ids);
        }

        // ARIA required parents
        Set<String> requiredParents = REQUIRED_ROLE_PARENT_BY_CHILD.get(role);
        if (requiredParents != null) {
            if (!requiredParents.contains(parentRole)) {
                err("An element with \u201Crole=" + role
                        + "\u201D requires " + renderRoleSet(requiredParents)
                        + " on the parent.");
            }
        }

        // ARIA only allowed children
        Set<String> allowedChildren = ALLOWED_CHILD_ROLE_BY_PARENT.get(parentRole);
        if (allowedChildren != null) {
            if (!allowedChildren.contains(role)) {
                err("Only elements with "
                        + renderRoleSet(allowedChildren)
                        + " are allowed as children of an element with \u201Crole="
                        + parentRole + "\u201D.");
            }
        }

        // ARIA row
        if ("row".equals(role)) {
            if (!("grid".equals(parentRole) || "treegrid".equals(parentRole) || (currentPtr > 1
                    && "grid".equals(stack[currentPtr - 1].getRole()) || "treegrid".equals(stack[currentPtr - 1].getRole())))) {
                err("An element with \u201Crole=row\u201D requires \u201Crole=treegrid\u201D or \u201Crole=grid\u201D on the parent or grandparent.");
            }
        }

        // ARIA IDREFS
        for (String att : MUST_NOT_DANGLE_IDREFS) {
            String attVal = atts.getValue("", att);
            if (attVal != null) {
                String[] tokens = AttributeUtil.split(attVal);
                for (int i = 0; i < tokens.length; i++) {
                    String token = tokens[i];
                    ariaReferences.add(new IdrefLocator(getDocumentLocator(),
                            token, att));
                }
            }
        }
        allIds.addAll(ids);

        // activedescendant
        for (Iterator<Map.Entry<StackNode, Locator>> iterator = openActiveDescendants.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<StackNode, Locator> entry = iterator.next();
            if (ids.contains(entry.getKey().getActiveDescendant())) {
                iterator.remove();
            }
        }
        
        if ("http://www.w3.org/1999/xhtml" == uri) {
            int number = specialAncestorNumber(localName);
            if (number > -1) {
                ancestorMask |= (1 << number);
            }
            if (repeat) {
                ancestorMask |= REPEAT_MASK;
            }
            if ("a" == localName && href) {
                ancestorMask |= HREF_MASK;
            }
            StackNode child = new StackNode(ancestorMask, localName, role, activeDescendant);
            if (activeDescendant != null) {
                openActiveDescendants.put(child, new LocatorImpl(getDocumentLocator()));
            }
            if ("select" == localName && atts.getIndex("", "multiple") > -1) {
                openSingleSelects.put(child, getDocumentLocator());
            } else if ("header" == localName) {
                openHeaders.put(child, new LocatorImpl(getDocumentLocator()));
            }
            push(child);
        } else {
            StackNode child = new StackNode(ancestorMask, null, role, activeDescendant);
            if (activeDescendant != null) {
                openActiveDescendants.put(child, new LocatorImpl(getDocumentLocator()));
            }
            push(child);
        }
        
        processChildContent(parent, skipDatagridCheck);
    }

    private void processChildContent(StackNode parent, boolean skipDatagridCheck) throws SAXException {
        if (parent == null) {
            return;
        }
        parent.setChildren();
        if (skipDatagridCheck) {
            return;
        }
        String datagridFirstChild;
        if ((datagridFirstChild = parent.getDatagridFirstChild()) != null) {
            err("When a \u201C" + datagridFirstChild + "\u201D is the first child of \u201Cdatagrid\u201D, it must not have following siblings.");
            parent.setDatagridFirstChild(null);
        }
    }

    /**
     * @see org.whattf.checker.Checker#characters(char[], int, int)
     */
    @Override public void characters(char[] ch, int start, int length)
            throws SAXException {
        for (int i = start; i < length; i++) {
            char c = ch[i];
            switch (c) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    continue;
                default:
                    processChildContent(peek(), false);
                    return;
            }
        }
    }

    private CharSequence renderRoleSet(Set<String> requiredParents) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (String role : requiredParents) {
            if (first) {
                first = false;
            } else {
                sb.append(" or ");
            }
            sb.append("\u201Crole=");
            sb.append(role);
            sb.append('\u201D');
        }
        return sb;
    }

}