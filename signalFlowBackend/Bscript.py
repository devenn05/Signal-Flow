import os

def gather_springboot_context(output_filename="signalFlowBackend.txt"):
    # Files we want to capture
    valid_extensions = ('.java', '.properties', '.xml', '.yml', '.yaml', '.sql', '.html')
    # Folders we want to skip
    ignored_folders = {'target', '.git', '.idea', 'node_modules', '.settings', 'bin'}

    with open(output_filename, 'w', encoding='utf-8') as output:
        # 1. Write the File Structure first
        output.write("================================================\n")
        output.write("PROJECT STRUCTURE\n")
        output.write("================================================\n")
        
        for root, dirs, files in os.walk('.'):
            # Skip ignored folders
            dirs[:] = [d for d in dirs if d not in ignored_folders]
            
            level = root.replace('.', '').count(os.sep)
            indent = ' ' * 4 * (level)
            output.write(f"{indent}{os.path.basename(root)}/\n")
            sub_indent = ' ' * 4 * (level + 1)
            for f in files:
                output.write(f"{sub_indent}{f}\n")
        
        output.write("\n\n")

        # 2. Capture pom.xml
        if os.path.exists('pom.xml'):
            output.write("================================================\n")
            output.write("FILE: pom.xml\n")
            output.write("================================================\n")
            with open('pom.xml', 'r', encoding='utf-8') as f:
                output.write(f.read())
            output.write("\n\n")

        # 3. Capture all source files
        for root, dirs, files in os.walk('src'):
            dirs[:] = [d for d in dirs if d not in ignored_folders]
            
            for file in files:
                if file.endswith(valid_extensions):
                    file_path = os.path.join(root, file)
                    output.write("================================================\n")
                    output.write(f"FILE: {file_path}\n")
                    output.write("================================================\n")
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            output.write(f.read())
                    except Exception as e:
                        output.write(f"Error reading file: {e}")
                    
                    output.write("\n\n")

    print(f"✅ Success! Your project context is ready in: {output_filename}")

if __name__ == "__main__":
    gather_springboot_context()