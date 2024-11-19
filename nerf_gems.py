import json
import glob
import os

def round_to_half(value):
    return max(0.5, round(value * 2) / 2)

def round_to_quarter_percent(value):
    return round(value * 400) / 400

def reduce_value(value, operation=None, reduction_factor=0.3):
    if isinstance(value, int):
        return max(1, round(value * 0.5))
    else:
        reduced = value * reduction_factor
        if operation == "ADDITION":
            return round_to_half(reduced)
        else:  # MULTIPLY_BASE, MULTIPLY_TOTAL, etc.
            return round_to_quarter_percent(reduced)

def process_gem_file(data):
    if 'bonuses' in data:
        for bonus in data['bonuses']:
            if 'values' in bonus:
                operation = bonus.get('operation', None)
                if isinstance(bonus['values'], dict):
                    for rarity, value in bonus['values'].items():
                        if isinstance(value, (int, float)):
                            bonus['values'][rarity] = reduce_value(value, operation)

def process_json_files(directory):
    json_files = glob.glob(f"{directory}/**/*.json", recursive=True)
    
    for file_path in json_files:
        try:
            with open(file_path, 'r') as f:
                data = json.load(f)
            
            process_gem_file(data)
            
            with open(file_path, 'w') as f:
                json.dump(data, f, indent='\t', separators=(',', ': '))
                
            print(f"Processed: {file_path}")
            
        except Exception as e:
            print(f"Error processing {file_path}: {e}")

if __name__ == "__main__":
    directory = "src/main/resources/data/apotheosis/gems"
    process_json_files(directory)