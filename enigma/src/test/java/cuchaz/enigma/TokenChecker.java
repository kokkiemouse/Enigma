/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import com.google.common.collect.Lists;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.source.*;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class TokenChecker {
	private final Decompiler decompiler;

	protected TokenChecker(Path path) throws IOException {
		CachingClassProvider classProvider = new CachingClassProvider(new JarClassProvider(path));
		decompiler = Decompilers.PROCYON.create(classProvider, new SourceSettings(false, false));
	}

	protected String getDeclarationToken(Entry<?> entry) {
		// decompile the class
		Source source = decompiler.getSource(entry.getContainingClass().getFullName());
		// DEBUG
		// tree.acceptVisitor( new TreeDumpVisitor( new File( "tree." + entry.getClassName().replace( '/', '.' ) + ".txt" ) ), null );
		String string = source.asString();
		SourceIndex index = source.index();

		// get the token value
		Token token = index.getDeclarationToken(entry);
		if (token == null) {
			return null;
		}
		return string.substring(token.start, token.end);
	}

	@SuppressWarnings("unchecked")
	protected Collection<String> getReferenceTokens(EntryReference<? extends Entry<?>, ? extends Entry<?>> reference) {
		// decompile the class
		Source source = decompiler.getSource(reference.context.getContainingClass().getFullName());
		String string = source.asString();
		SourceIndex index = source.index();

		// get the token values
		List<String> values = Lists.newArrayList();
		for (Token token : index.getReferenceTokens((EntryReference<Entry<?>, Entry<?>>) reference)) {
			values.add(string.substring(token.start, token.end));
		}
		return values;
	}
}
