/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.Token;
import cuchaz.enigma.ClassFile;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;

public class Gui
{
	private static final String Name = "Enigma";
	
	// controls
	private JFrame m_frame;
	private JList<ClassFile> m_obfClasses;
	private JList<ClassFile> m_deobfClasses;
	private JEditorPane m_editor;
	private JPanel m_actionPanel;
	private JPanel m_renamePanel;
	private JLabel m_typeLabel;
	private JTextField m_nameField;
	private JButton m_renameButton;
	
	// listeners
	private ClassSelectionListener m_classSelectionListener;
	private RenameListener m_renameListener;
	
	private BoxHighlightPainter m_highlightPainter;
	private Entry m_selectedEntry;
	
	public Gui( )
	{
		// init frame
		m_frame = new JFrame( Name );
		final Container pane = m_frame.getContentPane();
		pane.setLayout( new BorderLayout() );
		
		// init obfuscated classes list
		m_obfClasses = new JList<ClassFile>();
		m_obfClasses.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		m_obfClasses.setLayoutOrientation( JList.VERTICAL );
		m_obfClasses.setCellRenderer( new ObfuscatedClassListCellRenderer() );
		m_obfClasses.addMouseListener( new MouseAdapter()
		{
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					if( m_classSelectionListener != null )
					{
						ClassFile selected = m_obfClasses.getSelectedValue();
						if( selected != null )
						{
							m_classSelectionListener.classSelected( selected );
						}
					}
				}
			}
		} );
		JScrollPane obfScroller = new JScrollPane( m_obfClasses );
		JPanel obfPanel = new JPanel();
		obfPanel.setLayout( new BorderLayout() );
		obfPanel.add( new JLabel( "Obfuscated Classes" ), BorderLayout.NORTH );
		obfPanel.add( obfScroller, BorderLayout.CENTER );
		
		// init deobfuscated classes list
		m_deobfClasses = new JList<ClassFile>();
		m_obfClasses.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		m_obfClasses.setLayoutOrientation( JList.VERTICAL );
		JScrollPane deobfScroller = new JScrollPane( m_deobfClasses );
		JPanel deobfPanel = new JPanel();
		deobfPanel.setLayout( new BorderLayout() );
		deobfPanel.add( new JLabel( "De-obfuscated Classes" ), BorderLayout.NORTH );
		deobfPanel.add( deobfScroller, BorderLayout.CENTER );
		
		// init action panel
		m_actionPanel = new JPanel();
		m_actionPanel.setLayout( new BoxLayout( m_actionPanel, BoxLayout.Y_AXIS ) );
		m_actionPanel.setPreferredSize( new Dimension( 0, 120 ) );
		m_actionPanel.setBorder( BorderFactory.createTitledBorder( "Identifier Info" ) );
		m_nameField = new JTextField( 26 );
		m_renameButton = new JButton( "Rename" );
		m_renameButton.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent event )
			{
				if( m_renameListener != null && m_selectedEntry != null )
				{
					m_renameListener.rename( m_selectedEntry, m_nameField.getText() );
				}
			}
		} );
		m_renamePanel = new JPanel();
		m_renamePanel.setLayout( new FlowLayout( FlowLayout.LEFT, 6, 0 ) );
		m_typeLabel = new JLabel( "LongName:", JLabel.RIGHT );
		// NOTE: this looks ridiculous, but it fixes the label size to the size of current text
		m_typeLabel.setPreferredSize( m_typeLabel.getPreferredSize() );
		m_renamePanel.add( m_typeLabel );
		m_renamePanel.add( m_nameField );
		m_renamePanel.add( m_renameButton );
		clearEntry();
		
		// init editor
		DefaultSyntaxKit.initKit();
		m_editor = new JEditorPane();
		m_editor.setEditable( false );
		JScrollPane sourceScroller = new JScrollPane( m_editor );
		m_editor.setContentType( "text/java" );
		
		// layout controls
		JSplitPane splitLeft = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true, obfPanel, deobfPanel );
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout( new BorderLayout() );
		rightPanel.add( m_actionPanel, BorderLayout.NORTH );
		rightPanel.add( sourceScroller, BorderLayout.CENTER );
		JSplitPane splitMain = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, splitLeft, rightPanel );
		pane.add( splitMain, BorderLayout.CENTER );
		
		// show the frame
		pane.doLayout();
		m_frame.setSize( 800, 600 );
		m_frame.setMinimumSize( new Dimension( 640, 480 ) );
		m_frame.setVisible( true );
		m_frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
		
		// init listeners
		m_classSelectionListener = null;
		m_renameListener = null;
		
		m_highlightPainter = new BoxHighlightPainter();
	}
	
	public void setTitle( String title )
	{
		m_frame.setTitle( Name + " - " + title );
	}
	
	public void setObfClasses( List<ClassFile> classes )
	{
		m_obfClasses.setListData( new Vector<ClassFile>( classes ) );
	}
	
	public void setSource( String source )
	{
		setSource( source, null );
	}
	
	public void setSource( String source, SourceIndex index )
	{
		m_editor.setText( source );
	}
	
	public void highlightTokens( Iterable<Token> tokens )
	{
		// remove any old highlighters
		m_editor.getHighlighter().removeAllHighlights();
		
		if( tokens == null )
		{
			return;
		}
		
		// color things based on the index
		for( Token token : tokens )
		{
			try
			{
				m_editor.getHighlighter().addHighlight( token.start, token.end(), m_highlightPainter );
			}
			catch( BadLocationException ex )
			{
				throw new IllegalArgumentException( ex );
			}
		}
		
		redraw();
	}
	
	public void setClassSelectionListener( ClassSelectionListener val )
	{
		m_classSelectionListener = val;
	}
	
	public void setRenameListener( RenameListener val )
	{
		m_renameListener = val;
	}
	
	public void setCaretListener( CaretListener listener )
	{
		// remove any old listeners
		for( CaretListener oldListener : m_editor.getCaretListeners() )
		{
			m_editor.removeCaretListener( oldListener );
		}
		
		m_editor.addCaretListener( listener );
	}
	
	public void clearEntry( )
	{
		m_actionPanel.removeAll();
		JLabel label = new JLabel( "No identifier selected" );
		unboldLabel( label );
		label.setHorizontalAlignment( JLabel.CENTER );
		m_actionPanel.add( label );
		
		redraw();
	}

	public void showEntry( Entry entry )
	{
		if( entry == null )
		{
			clearEntry();
			return;
		}
		
		// layout the action panel
		m_actionPanel.removeAll();
		m_actionPanel.add( m_renamePanel );
		m_nameField.setText( entry.getName() );
		
		// layout the dynamic section
		JPanel dynamicPanel = new JPanel();
		dynamicPanel.setLayout( new GridLayout( 3, 1, 0, 0 ) );
		m_actionPanel.add( dynamicPanel );
		if( entry instanceof ClassEntry )
		{
			showEntry( (ClassEntry)entry, dynamicPanel );
		}
		else if( entry instanceof FieldEntry )
		{
			showEntry( (FieldEntry)entry, dynamicPanel );
		}
		else if( entry instanceof MethodEntry )
		{
			showEntry( (MethodEntry)entry, dynamicPanel );
		}
		else if( entry instanceof ArgumentEntry )
		{
			showEntry( (ArgumentEntry)entry, dynamicPanel );
		}
		else
		{
			throw new Error( "Unknown entry type: " + entry.getClass().getName() );
		}
		
		redraw();
	}
	
	public void showEntry( ClassEntry entry, JPanel panel )
	{
		m_typeLabel.setText( "Class: " );
	}
	
	public void showEntry( FieldEntry entry, JPanel panel )
	{
		m_typeLabel.setText( "Field: " );
		addNameValue( panel, "Class", entry.getClassEntry().getName() );
	}
	
	public void showEntry( MethodEntry entry, JPanel panel )
	{
		m_typeLabel.setText( "Method: " );
		addNameValue( panel, "Class", entry.getClassEntry().getName() );
		addNameValue( panel, "Signature", entry.getSignature() );
	}
	
	public void showEntry( ArgumentEntry entry, JPanel panel )
	{
		m_typeLabel.setText( "Argument: " );
		addNameValue( panel, "Class", entry.getMethodEntry().getClassEntry().getName() );
		addNameValue( panel, "Method", entry.getMethodEntry().getName() );
		addNameValue( panel, "Index", Integer.toString( entry.getIndex() ) );
	}
	
	private void addNameValue( JPanel container, String name, String value )
	{
		JPanel panel = new JPanel();
		panel.setLayout( new FlowLayout( FlowLayout.LEFT, 6, 0 ) );
		container.add( panel );
		
		JLabel label = new JLabel( name + ":", JLabel.RIGHT );
		label.setPreferredSize( new Dimension( 80, label.getPreferredSize().height ) );
		panel.add( label );
		
		panel.add( unboldLabel( new JLabel( value, JLabel.LEFT ) ) );
	}

	private JLabel unboldLabel( JLabel label )
	{
		Font font = label.getFont();
		label.setFont( font.deriveFont( font.getStyle() & ~Font.BOLD ) );
		return label;
	}
	
	private void redraw( )
	{
		m_frame.validate();
		m_frame.repaint();
	}
}