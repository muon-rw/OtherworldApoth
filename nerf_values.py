import json
import glob
import os
def round_to_quarter_percent(value):
    return round(value * 400) / 400

def reduce_values(data, factor=0.25):
    if isinstance(data, dict):
        for key, value in data.items():
            if key in ['min', 'step'] and isinstance(value, (int, float)):
                reduced_value = value * factor
                data[key] = round_to_quarter_percent(reduced_value)
            elif isinstance(value, (dict, list)):
                reduce_values(value, factor)
    elif isinstance(data, list):
        for item in data:
            if isinstance(item, (dict, list)):
                reduce_values(item, factor)

def process_json_files(directory):
    json_files = glob.glob(f"{directory}/**/*.json", recursive=True)
    
    for file_path in json_files:
        try:
            with open(file_path, 'r') as f:
                data = json.load(f)
            
            reduce_values(data)
            
            with open(file_path, 'w') as f:
                json.dump(data, f, indent='\t', separators=(',', ': '))
                
            print(f"Processed: {file_path}")
            
        except Exception as e:
            print(f"Error processing {file_path}: {e}")

process_json_files("src/main/resources/data/apotheosis/affixes")