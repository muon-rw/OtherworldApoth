import json
import glob
import os

def round_to_quarter(value):
    # For larger values, round to nearest 0.25
    return round(value * 4) / 4

def round_to_quarter_percent(value):
    # For small values (0-1 range), round to nearest 0.0025
    return round(value * 400) / 400

def reduce_values(data, factor=0.25):
    if isinstance(data, dict):
        for key, value in data.items():
            if key == 'min' and isinstance(value, (int, float)):
                # For small values (< 1), use more precise rounding
                if value < 1:
                    data[key] = round_to_quarter_percent(value * factor)
                else:
                    # For larger values, round to 0.25
                    data[key] = round_to_quarter(value * factor)
            elif key == 'step' and isinstance(value, (int, float)):
                # For small steps, preserve their precision
                if value < 0.1:
                    data[key] = round_to_quarter_percent(value)
                else:
                    # Only reduce larger steps
                    data[key] = round_to_quarter_percent(value * factor)
            elif key == 'steps' and isinstance(value, (int, float)):
                # Only reduce steps if they're > 2
                if value > 2:
                    data[key] = max(2, round(value * factor))
                else:
                    data[key] = value
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

if __name__ == "__main__":
    directory = "src/main/resources/data/apotheosis/affixes"
    process_json_files(directory)