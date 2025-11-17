#!/usr/bin/env python3
"""
Convert PROJECT_REPORT.md to PDF format.
"""
import sys
import os

try:
    import markdown
    from weasyprint import HTML, CSS
    from weasyprint.text.fonts import FontConfiguration
except ImportError as e:
    print(f"Required library not found: {e}")
    print("\nInstalling required packages...")
    os.system("pip install markdown weasyprint")
    import markdown
    from weasyprint import HTML, CSS
    from weasyprint.text.fonts import FontConfiguration

def markdown_to_pdf(md_file, pdf_file):
    """Convert Markdown file to PDF."""
    try:
        # Read Markdown file
        with open(md_file, 'r', encoding='utf-8') as f:
            md_content = f.read()
        
        # Convert Markdown to HTML
        html_content = markdown.markdown(
            md_content,
            extensions=['tables', 'fenced_code', 'toc']
        )
        
        # Create HTML document with styling
        html_document = f"""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Memory Match Game - Project Report</title>
    <style>
        @page {{
            size: A4;
            margin: 2cm;
        }}
        body {{
            font-family: 'Arial', sans-serif;
            line-height: 1.6;
            color: #333;
        }}
        h1 {{
            color: #667eea;
            border-bottom: 3px solid #667eea;
            padding-bottom: 10px;
            page-break-after: avoid;
        }}
        h2 {{
            color: #764ba2;
            border-bottom: 2px solid #764ba2;
            padding-bottom: 5px;
            margin-top: 30px;
            page-break-after: avoid;
        }}
        h3 {{
            color: #555;
            margin-top: 20px;
            page-break-after: avoid;
        }}
        code {{
            background-color: #f4f4f4;
            padding: 2px 5px;
            border-radius: 3px;
            font-family: 'Courier New', monospace;
        }}
        pre {{
            background-color: #f4f4f4;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
            page-break-inside: avoid;
        }}
        table {{
            border-collapse: collapse;
            width: 100%;
            margin: 20px 0;
            page-break-inside: avoid;
        }}
        th, td {{
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }}
        th {{
            background-color: #667eea;
            color: white;
        }}
        tr:nth-child(even) {{
            background-color: #f9f9f9;
        }}
        .toc {{
            page-break-after: always;
        }}
        .toc ul {{
            list-style-type: none;
            padding-left: 20px;
        }}
        .toc a {{
            text-decoration: none;
            color: #667eea;
        }}
        p {{
            text-align: justify;
        }}
        ul, ol {{
            margin: 10px 0;
            padding-left: 30px;
        }}
        li {{
            margin: 5px 0;
        }}
        hr {{
            border: none;
            border-top: 2px solid #667eea;
            margin: 30px 0;
        }}
    </style>
</head>
<body>
    {html_content}
</body>
</html>
        """
        
        # Convert HTML to PDF
        print(f"Converting {md_file} to PDF...")
        HTML(string=html_document).write_pdf(pdf_file)
        print(f"✓ Successfully created {pdf_file}")
        return True
        
    except Exception as e:
        print(f"Error converting to PDF: {e}")
        return False

if __name__ == "__main__":
    md_file = "PROJECT_REPORT.md"
    pdf_file = "PROJECT_REPORT.pdf"
    
    if not os.path.exists(md_file):
        print(f"Error: {md_file} not found!")
        sys.exit(1)
    
    success = markdown_to_pdf(md_file, pdf_file)
    sys.exit(0 if success else 1)

