import json
import glob
import os

def reduce_values(data, factor=0.25):
    if isinstance(data, dict):
        for key, value in data.items():
            if key in ['min', 'step'] and isinstance(value, (int, float)):
                data[key] = round(value * factor, 3)  # Round to 3 decimal places
            elif isinstance(value, (dict, list)):
                reduce_values(value, factor)
    elif isinstance(data, list):
        for item in data:
            if isinstance(item, (dict, list)):
                reduce_values(item, factor)

def process_json_files(directory):
    # Find all JSON files in the directory and subdirectories
    json_files = glob.glob(f"{directory}/**/*.json", recursive=True)
    
    for file_path in json_files:
        try:
            with open(file_path, 'r') as f:
                data = json.load(f)
            
            # Modify the values
            reduce_values(data)
            
            # Write back to file with pretty printing
            with open(file_path, 'w') as f:
                json.dump(data, f, indent='\t')
                
            print(f"Processed: {file_path}")
            
        except Exception as e:
            print(f"Error processing {file_path}: {e}")

# Run the script on your data directory
process_json_files("src/main/resources/data/apotheosis/affixes")